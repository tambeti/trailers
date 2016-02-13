package ee.it.trailers

import android.support.v7.widget.RecyclerView
import android.util.JsonReader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.picasso.Picasso
import ee.it.trailers.tmdb.DiscoverResult
import ee.it.trailers.tmdb.Genres
import ee.it.trailers.tmdb.Movie
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions
import java.io.IOException

class MoviesAdapter(app: MyApplication, private val mYear: Int, private val mGenreFilter: List<Int>,
                    private val mMovieSelectedListener: MoviesAdapter.OnMovieSelectedListener):
        RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {
    interface OnMovieSelectedListener {
        fun onMovieSelected(movieObservable: Observable<Movie>)
    }

    private val PAGE_SIZE = 20
    private val mHttpClient: OkHttpClient
    private val mPicasso: Picasso
    private val mGenres: Observable<Genres>
    private val mObservables: MutableList<Observable<List<Movie>>> = mutableListOf()
    private var mCount: Int = 0

    init {
        Log.i("foobar", "creating adapter")
        mHttpClient = app.httpClient
        mPicasso = app.picasso
        mGenres = app.genres

        initObservables()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_item,
                parent, false)
        return ViewHolder(view, mMovieSelectedListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindMovie(loadMovie(position), mGenres, mPicasso)
    }

    override fun getItemCount() = mCount

    override fun onViewRecycled(holder: ViewHolder?) {
        holder!!.reset(mPicasso)
        super.onViewRecycled(holder)
    }

    fun loadMovie(position: Int): Observable<Movie> {
        val page = position / PAGE_SIZE
        val positionInPage = position % PAGE_SIZE

        return mObservables[page].map { movies -> movies[positionInPage] }
    }

    private fun initObservables() {
        discoverMovies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { discoverResult ->
                    mCount = discoverResult.totalResults

                    val page0 = Observable.just(discoverResult)
                            .map { discoverResult -> discoverResult.results }
                            .cache(1)

                    mObservables.add(page0)

                    for (page in 0..discoverResult.totalPages - 1) {
                        val o = loadPage(page + 2).cache(1)
                        mObservables.add(o)
                    }

                    notifyDataSetChanged()
                }
    }

    internal fun loadPage(page: Int): Observable<List<Movie>> {
        return discoverMovies(page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { discoverResult -> discoverResult.results }
    }

    private fun discoverMovies(page: Int = 0): Observable<DiscoverResult> {
        return Observable.create { subscriber ->
            Log.i("foobar", "Loading page " + page)
            val urlBuilder = MyApplication.apiUrlBuilder()
                    .addPathSegment("discover")
                    .addPathSegment("movie")
                    .addQueryParameter("primary_release_year", Integer.toString(mYear))
                    .addQueryParameter("vote_count.gte", "50")
                    .addQueryParameter("language", "en")
                    .addQueryParameter("sort_by", "primary_release_date.asc")

            if (!mGenreFilter.isEmpty()) {
                urlBuilder.addQueryParameter("with_genres", mGenreFilter.joinToString(","))
            }

            if (page > 0) {
                urlBuilder.addQueryParameter("page", Integer.toString(page))
            }

            val request = Request.Builder()
                    .url(urlBuilder.build())
                    .build()

            try {
                val response = mHttpClient.newCall(request).execute()
                val result = JsonReader(response.body().charStream()).use {
                    DiscoverResult.Builder(it).build()
                }
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(result)
                    subscriber.onCompleted()
                }
            } catch (e: IOException) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        }
    }

    class ViewHolder(view: View, listener: OnMovieSelectedListener?) : RecyclerView.ViewHolder(view) {
        private val mSub = SerialSubscription()
        private var mObservable = Observable.empty<Movie>()

        val poster: ImageView
        val title: TextView
        val genre: TextView
        val releaseDate: TextView

        init {
            poster = view.findViewById(R.id.poster) as ImageView
            title = view.findViewById(R.id.title) as TextView
            genre = view.findViewById(R.id.genre) as TextView
            releaseDate = view.findViewById(R.id.release_date) as TextView

            view.setOnClickListener {
                listener?.onMovieSelected(mObservable)
            }
        }

        fun bindMovie(movieObservable: Observable<Movie>, genresObservable: Observable<Genres>,
                      picasso: Picasso) {
            mObservable = movieObservable

            val sub = Observable.zip(genresObservable, movieObservable) { genres, movie -> Pair(movie, genres) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { data -> bindMovie(data.first, data.second, picasso) }

            mSub.set(sub)
        }

        fun bindMovie(movie: Movie, genres: Genres, picasso: Picasso) {
            title.text = movie.title
            releaseDate.text = movie.releaseDate
            genre.text = movie.genreIds
                    .map { genres.name(it)?.name }
                    .joinToString(" / ")

            picasso.load(MyApplication.POSTERS_URL_MINI + movie.posterPath)
                    .resize(185, 185)
                    .centerCrop()
                    .into(poster)
        }

        fun reset(picasso: Picasso) {
            picasso.cancelRequest(poster)
            mSub.set(Subscriptions.empty())
            mObservable = Observable.empty<Movie>()
        }
    }
}

package ee.it.trailers

import android.support.v7.widget.RecyclerView
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import ee.it.trailers.tmdb.DiscoverResult
import ee.it.trailers.tmdb.Genres
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.movie_item.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions

class MoviesAdapter(app: MyApplication, private val mYear: Int, private val mGenreFilter: List<Int>,
                    private val mMovieSelectedListener: MoviesAdapter.OnMovieSelectedListener):
        RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {
    interface OnMovieSelectedListener {
        fun onMovieSelected(movieObservable: Observable<Movie>)
    }

    private val PAGE_SIZE = 20
    private val observables: MutableList<Observable<List<Movie>>> = mutableListOf()
    private var count: Int = 0
    private val httpClient by lazy { app.httpClient }
    private val picasso by lazy { app.picasso }
    private val genres by lazy { app.genres }

    init {
        initObservables()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_item,
                parent, false)
        return ViewHolder(view, mMovieSelectedListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindMovie(loadMovie(position), genres, picasso)
    }

    override fun getItemCount() = count

    override fun onViewRecycled(holder: ViewHolder?) {
        holder!!.reset(picasso)
        super.onViewRecycled(holder)
    }

    fun loadMovie(position: Int): Observable<Movie> {
        val page = position / PAGE_SIZE
        val positionInPage = position % PAGE_SIZE

        return observables[page].map { movies -> movies[positionInPage] }
    }

    private fun initObservables() {
        discoverMovies()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { discoverResult ->
                    count = discoverResult.totalResults

                    val page0 = Observable.just(discoverResult)
                            .map { discoverResult -> discoverResult.results }
                            .cache(1)

                    observables.add(page0)

                    for (page in 0..discoverResult.totalPages - 1) {
                        val o = loadPage(page + 2).cache(1)
                        observables.add(o)
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

        return httpClient.get(urlBuilder.build().toString())
                .map { response ->
                    JsonReader(response.body().charStream()).use {
                        DiscoverResult.Builder(it).build()
                    }
                }
                .toObservable()
    }

    class ViewHolder(val view: View, listener: OnMovieSelectedListener?) : RecyclerView.ViewHolder(view) {
        private val mSub = SerialSubscription()
        private var mObservable = Observable.empty<Movie>()

        init {
            view.setOnClickListener {
                listener?.onMovieSelected(mObservable)
            }
        }

        fun bindMovie(movieObservable: Observable<Movie>, genresObservable: Observable<Genres>,
                      picasso: Picasso) {
            mObservable = movieObservable

            val sub = Observable.zip(genresObservable, movieObservable) { genres, movie -> movie to genres }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { data -> bindMovie(data.first, data.second, picasso) }

            mSub.set(sub)
        }

        fun bindMovie(movie: Movie, genres: Genres, picasso: Picasso) {
            view.title.text = movie.title
            view.release_date.text = movie.releaseDate
            view.genre.text = movie.genreIds
                    .map { genres.name(it)?.name }
                    .joinToString(" / ")

            picasso.load(MyApplication.POSTERS_URL_MINI + movie.posterPath)
                    .resize(185, 185)
                    .centerCrop()
                    .into(view.poster)
        }

        fun reset(picasso: Picasso) {
            picasso.cancelRequest(view.poster)
            mSub.set(Subscriptions.empty())
            mObservable = Observable.empty<Movie>()
        }
    }
}

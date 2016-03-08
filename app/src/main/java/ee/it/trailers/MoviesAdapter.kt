package ee.it.trailers

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import ee.it.trailers.tmdb.Genres
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.movie_item.view.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions

class MoviesAdapter(app: MyApplication, private val mMovieSelectedListener: MoviesAdapter.OnMovieSelectedListener):
        RecyclerView.Adapter<MoviesAdapter.ViewHolder>() {
    interface OnMovieSelectedListener {
        fun onMovieSelected(movieObservable: Observable<Movie>)
    }

    private val picasso by lazy { app.picasso }
    private val genres by lazy { app.genres }
    private var data: MoviesData = MoviesData.EMPTY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.movie_item,
                parent, false)
        return ViewHolder(view, mMovieSelectedListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindMovie(data.loadMovie(position), genres, picasso)
    }

    override fun getItemCount() = data.count

    override fun onViewRecycled(holder: ViewHolder?) {
        holder!!.reset(picasso)
        super.onViewRecycled(holder)
    }

    fun replaceData(data: MoviesData) {
        this.data = data
        notifyDataSetChanged()
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

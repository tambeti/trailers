package ee.it.trailers

import android.content.Context
import android.util.JsonReader
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import ee.it.trailers.tmdb.Movie
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.IOException

interface MovieDetailsView {
    fun setTitle(value: String)
    fun setPoster(value: String)
    fun setGenres(value: String)
    fun setRuntime(value: String)
    fun setDirector(value: String)
    fun setWriter(value: String)
    fun setCast(value: String)
    fun setTagline(value: String)
    fun setPlot(value: String)
    fun setHaveTrailer(value: Boolean)

    fun showTorrentFinder(movie: Movie)
    fun openMagnet(magnet: String)
}

class MovieDetailsPresenter(val context: Context, val httpClient: OkHttpClient, val movie: Movie) :
        Presenter<MovieDetailsView> {
    private val pulsar: Pulsar
    private val sub: Subscription
    private var view: MovieDetailsView? = null
    var movieDetails: Movie? = null

    init {
        val app = context.applicationContext as MyApplication
        pulsar = Pulsar(app, httpClient)

        sub = movieDetails(movie.id).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { movie ->
                    movieDetails = movie
                    updateView()
                }
    }

    override fun bindView(view: MovieDetailsView) {
        this.view = view
        updateView()
    }

    override fun unbindView() {
        view = null
    }

    override fun destroy() {
        sub.unsubscribe()
    }

    fun onPlayTrailerClicked() {
        movieDetails?.trailers
                ?.firstOrNull()
                ?.let { firstTrailer ->
                    val url = pulsar.trailerUrl(firstTrailer.source)
                    Kodi.play(context, httpClient, url)
                }
    }

    fun onPlayClicked() {
        view?.showTorrentFinder(movie)
    }

    fun onTorrentSelected(name: String, magnet: String, action: TorrentPickerFragment.Action) {
        when (action) {
            TorrentPickerFragment.Action.PLAY -> pulsar.playUri(magnet)
            TorrentPickerFragment.Action.COPY -> view?.openMagnet(magnet)
        }
    }

    private fun updateView() {
        view?.let { v ->
            v.setTitle(movie.title)
            v.setPlot(movie.overview)
            v.setPoster(MyApplication.POSTERS_URL + movie.posterPath)

            movieDetails?.let { details ->
                v.setRuntime("${details.runtime} minutes")
                v.setGenres(details.genres
                        .map { it.name }
                        .joinToString())

                v.setDirector(details.crew
                        .filter { "Director".equals(it.job) }
                        .map { it.name }
                        .joinToString())

                v.setWriter(details.crew
                        .filter { "Director".equals(it.job) }
                        .map { it.name }
                        .joinToString())

                v.setCast(details.cast
                        .take(10)
                        .map { it.name }
                        .joinToString())

                v.setTagline(details.tagline)
                v.setHaveTrailer(!details.trailers.isEmpty())
            }
        }
    }

    private fun movieDetails(id: Long): Observable<Movie> {
        return Observable.create { subscriber ->
            val url = MyApplication.apiUrlBuilder()
                    .addPathSegment("movie")
                    .addPathSegment(id.toString())
                    .addQueryParameter("append_to_response", "trailers,credits")
                    .build()

            val request = Request.Builder()
                    .url(url)
                    .build()

            try {
                val response = httpClient.newCall(request).execute()
                val movie = JsonReader(response.body().charStream()).use {
                    Movie.Builder(it).build()
                }
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(movie)
                    subscriber.onCompleted()
                }
            } catch (e: IOException) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        }
    }
}
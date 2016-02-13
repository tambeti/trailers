package ee.it.trailers

import android.app.Fragment
import android.os.Bundle
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.squareup.okhttp.Request
import ee.it.trailers.tmdb.Movie
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.IOException


class MovieDetailsFragment : Fragment() {
    private val httpClient by lazy {
        (activity.applicationContext as MyApplication).httpClient
    }

    private val picasso by lazy {
        (activity.applicationContext as MyApplication).picasso
    }

    private val movieId by lazy { arguments!!.getLong(KEY_ID) }

    private var mPulsar: Pulsar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = activity.applicationContext as MyApplication
        mPulsar = Pulsar(app, httpClient)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.movie_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val poster = view.findViewById(R.id.poster) as ImageView
        val genre = view.findViewById(R.id.genres) as TextView
        val runtime = view.findViewById(R.id.runtime) as TextView
        val director = view.findViewById(R.id.director) as TextView
        val writer = view.findViewById(R.id.writer) as TextView
        val cast = view.findViewById(R.id.cast) as TextView

        val plot = view.findViewById(R.id.plot) as TextView
        val trailer = view.findViewById(R.id.trailer) as Button
        val play = view.findViewById(R.id.play) as Button

        trailer.isEnabled = false

        movieDetails(movieId).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread())
                .subscribe { movie ->
                    plot.text = movie.overview
                    runtime.text = "${movie.runtime} minutes"
                    genre.text = movie.genres
                            .map { it.name }
                            .joinToString()

                    director.text = movie.crew
                            .filter { "Director".equals(it.job) }
                            .map { it.name }
                            .joinToString()

                    writer.text = movie.crew
                            .filter { "Writer".equals(it.job) }
                            .map { it.name }
                            .joinToString()

                    cast.text = movie.cast
                            .take(10)
                            .map { it.name }
                            .joinToString()

                    movie.trailers.firstOrNull()?.let { firstTrailer ->
                        val url = mPulsar!!.trailerUrl(firstTrailer.source)
                        trailer.isEnabled = true
                        trailer.setOnClickListener { Kodi.play(activity, httpClient, url) }
                    }

                    play.setOnClickListener { mPulsar!!.play(movie.imdbId) }

                    picasso.load(MyApplication.POSTERS_URL + movie.posterPath).into(poster)
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

    companion object {
        private val KEY_ID = "movie-details-id"

        fun newInstance(movieId: Long) = MovieDetailsFragment().apply {
            arguments = Bundle().apply { putLong(KEY_ID, movieId) }
        }
    }
}

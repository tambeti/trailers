package ee.it.trailers

import android.app.Activity
import android.app.Fragment
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.squareup.okhttp.Request
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.movie_details.*
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

    private val movie by lazy { arguments!!.getSerializable(KEY_MOVIE) as Movie }

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

        /*
        val poster = view.findViewById(R.id.poster) as ImageView
        val genre = view.findViewById(R.id.genres) as TextView
        val runtime = view.findViewById(R.id.runtime) as TextView
        val director = view.findViewById(R.id.director) as TextView
        val writer = view.findViewById(R.id.writer) as TextView
        val cast = view.findViewById(R.id.cast) as TextView
        val tagline = view.findViewById(R.id.tagline) as TextView

        val plot = view.findViewById(R.id.plot) as TextView
        val trailer = view.findViewById(R.id.trailer) as Button
        val play = view.findViewById(R.id.play) as Button
        */

        trailer.isEnabled = false

        plot.text = movie.overview
        picasso.load(MyApplication.POSTERS_URL + movie.posterPath)
                .into(poster)

        movieDetails(movie.id).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { movie ->
                    runtime.text = "${movie.runtime} minutes"
                    genres.text = movie.genres
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

                    tagline.text = movie.tagline

                    movie.trailers.firstOrNull()?.let { firstTrailer ->
                        val url = mPulsar!!.trailerUrl(firstTrailer.source)
                        trailer.isEnabled = true
                        trailer.setOnClickListener { Kodi.play(activity, httpClient, url) }
                    }

                    play.setOnClickListener {
                        val f = TorrentPickerFragment.newInstance(movie)
                        f.setTargetFragment(this, REQUEST_CODE_TORRENT)
                        f.show(fragmentManager, "dialog")
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_TORRENT -> {
                data?.let {
                    val name = it.getStringExtra(TorrentPickerFragment.KEY_NAME)
                    val magnet = it.getStringExtra(TorrentPickerFragment.KEY_MAGNET)
                    val action = it.getSerializableExtra(TorrentPickerFragment.KEY_ACTION) as TorrentPickerFragment.Action

                    println("Selected $name")
                    when (action) {
                        TorrentPickerFragment.Action.PLAY -> mPulsar?.playUri(magnet!!)
                        TorrentPickerFragment.Action.COPY -> open(magnet!!)
                    }

                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun open(uri: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
        } catch (e: ActivityNotFoundException) {
            copy(uri)
        }
    }

    private fun copy(uri: String) {
        val clipboard = activity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText("label", uri)
        Toast.makeText(activity, R.string.magnet_copied, Toast.LENGTH_SHORT)
                .show()
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
        private val KEY_MOVIE = "movie-details-movie"
        private val REQUEST_CODE_TORRENT = 1

        fun newInstance(movie: Movie) = MovieDetailsFragment().apply {
            arguments = Bundle().apply { putSerializable(KEY_MOVIE, movie) }
        }
    }
}

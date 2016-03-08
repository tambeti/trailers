package ee.it.trailers

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.movie_details.*


class MovieDetailsFragment : Fragment(), LoaderManager.LoaderCallbacks<MovieDetailsPresenter>, MovieDetailsView {
    private val httpClient by lazy { (activity.applicationContext as MyApplication).httpClient }
    private val picasso by lazy { (activity.applicationContext as MyApplication).picasso }
    private val movie by lazy { arguments!!.getSerializable(KEY_MOVIE) as Movie }
    private var presenter: MovieDetailsPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.movie_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        play.setOnClickListener { presenter?.onPlayClicked() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(1, null, this)
    }

    override fun onResume() {
        super.onResume()
        presenter!!.bindView(this)
    }

    override fun onPause() {
        presenter!!.unbindView()
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_TORRENT -> {
                data?.let {
                    val name = it.getStringExtra(TorrentPickerFragment.KEY_NAME)
                    val magnet = it.getStringExtra(TorrentPickerFragment.KEY_MAGNET)
                    val action = it.getSerializableExtra(TorrentPickerFragment.KEY_ACTION) as TorrentPickerFragment.Action
                    presenter?.onTorrentSelected(name, magnet, action)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun setTitle(value: String) {
        activity.title = value
    }

    override fun setPoster(value: String) {
        picasso.load(value).into(poster)
    }

    override fun setGenres(value: String) {
        genres.text = value
    }

    override fun setRuntime(value: String) {
        runtime.text = value
    }

    override fun setDirector(value: String) {
        director.text = value
    }

    override fun setWriter(value: String) {
        writer.text = value
    }

    override fun setCast(value: String) {
        cast.text = value
    }

    override fun setTagline(value: String) {
        tagline.text = value
    }

    override fun setPlot(value: String) {
        plot.text = value
    }

    override fun setHaveTrailer(value: Boolean) {
        trailer.isEnabled = value
        if (value) {
            trailer.setOnClickListener { presenter?.onPlayTrailerClicked() }
        }
    }

    override fun showTorrentFinder(movie: Movie) {
        val f = TorrentPickerFragment.newInstance(movie)
        f.setTargetFragment(this, REQUEST_CODE_TORRENT)
        f.show(fragmentManager, "dialog")
    }

    override fun openMagnet(magnet: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(magnet)))
        } catch (e: ActivityNotFoundException) {
            val clipboard = activity.getSystemService(Activity.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText("label", magnet)
            Toast.makeText(activity, R.string.magnet_copied, Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<MovieDetailsPresenter> {
        return PresenterLoader(activity) { MovieDetailsPresenter(activity, httpClient, movie) }
    }

    override fun onLoadFinished(loader: Loader<MovieDetailsPresenter>?, presenter: MovieDetailsPresenter?) {
        this.presenter = presenter
    }

    override fun onLoaderReset(loader: Loader<MovieDetailsPresenter>?) {
        presenter = null
    }

    companion object {
        private val KEY_MOVIE = "movie-details-movie"
        private val REQUEST_CODE_TORRENT = 1

        fun newInstance(movie: Movie) = MovieDetailsFragment().apply {
            arguments = Bundle().apply { putSerializable(KEY_MOVIE, movie) }
        }
    }
}

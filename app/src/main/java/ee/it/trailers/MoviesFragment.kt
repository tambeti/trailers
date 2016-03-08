package ee.it.trailers

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.movies_fragment.view.*

class MoviesFragment : Fragment(), MoviesView,
        LoaderManager.LoaderCallbacks<MoviesPresenter> {
    companion object {
        private val REQUEST_CODE_PICK_YEAR = 1
        private val REQUEST_CODE_PICK_GENRE = 2
    }

    private lateinit var adapter: MoviesAdapter
    private var presenter: MoviesPresenter? = null
    private var year: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.movies_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.movies_list.let {
            it.layoutManager = LinearLayoutManager(view.context)
            it.setHasFixedSize(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setHasOptionsMenu(true)

        loaderManager.initLoader(1, null, this)
    }

    override fun onResume() {
        super.onResume()
        presenter!!.bindView(this)

        activity.title = getString(R.string.app_name)
    }

    override fun onPause() {
        presenter!!.unbindView()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.year_selector).apply {
            title = year
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.settings -> {
            presenter?.onPreferencesClicked()
            true
        }
        R.id.year_selector -> {
            presenter?.onYearClicked()
            true
        }
        R.id.genre_selector -> {
            presenter?.onGenresClicked()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CODE_PICK_YEAR -> {
                presenter?.setYear(data.getIntExtra(NumberPickerFragment.RESULT, -1))
            }
            REQUEST_CODE_PICK_GENRE -> {
                val array = data.getIntArrayExtra(GenrePickerFragment.RESULT)
                presenter?.setGenres(array.toList())
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun updateMovies(data: MoviesData) {
        adapter.replaceData(data)
    }

    override fun updateYear(year: String) {
        this.year = year
        activity.invalidateOptionsMenu()
    }

    override fun showMovieDetails(movie: Movie) {
        val fragment = MovieDetailsFragment.newInstance(movie)
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
    }

    override fun showPreferences() {
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment())
                .addToBackStack(null)
                .commit()
    }

    override fun showYearPicker(from: Int, to: Int, selected: Int) {
        val fragment = NumberPickerFragment.newInstance(from, to, selected)
        fragment.setTargetFragment(this, REQUEST_CODE_PICK_YEAR)
        fragment.show(fragmentManager, "year-picker")
    }

    override fun showGenrePicker(selected: List<Int>) {
        val fragment = GenrePickerFragment.newInstance(selected)
        fragment.setTargetFragment(this, REQUEST_CODE_PICK_GENRE)
        fragment.show(fragmentManager, "genre-picker")
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<MoviesPresenter> {
        return PresenterLoader(activity) { MoviesPresenter(activity) }
    }

    override fun onLoadFinished(loader: Loader<MoviesPresenter>, presenter: MoviesPresenter) {
        this.presenter = presenter

        val app = activity.applicationContext as MyApplication
        adapter = MoviesAdapter(app, presenter)
        view?.movies_list?.adapter = adapter
    }

    override fun onLoaderReset(loader: Loader<MoviesPresenter>) {
        presenter = null
    }
}

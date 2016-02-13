package ee.it.trailers

import android.app.Activity
import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import ee.it.trailers.tmdb.Movie
import rx.Observable
import java.util.*

class MoviesFragment : Fragment(), MoviesAdapter.OnMovieSelectedListener {
    companion object {
        private val REQUEST_CODE_PICK_YEAR = 1
        private val REQUEST_CODE_PICK_GENRE = 2
    }

    interface OnMovieSelected {
        fun onMovieSelected(movie: Movie)
    }

    private val mGenres: MutableList<Int> = mutableListOf()
    private var mListener: OnMovieSelected? = null
    private var mSelectedYear: Int = 0

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            mListener = activity as OnMovieSelected
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement OnArticleSelectedListener")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.movies_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.findViewById(R.id.movies_list) as RecyclerView).let {
            it.layoutManager = LinearLayoutManager(view.context)
            it.setHasFixedSize(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setHasOptionsMenu(true)

        activity.invalidateOptionsMenu()
        mGenres.clear()
        mGenres.addAll(Prefs.genres(activity))
        mSelectedYear = Prefs.year(activity)
    }

    override fun onResume() {
        super.onResume()
        reloadData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.year_selector).apply {
            title = mSelectedYear.toString()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
            R.id.year_selector -> {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val fragment = NumberPickerFragment.newInstance(1920, currentYear, mSelectedYear)
                fragment.setTargetFragment(this, REQUEST_CODE_PICK_YEAR)
                fragment.show(fragmentManager, "year-picker")
            }
            R.id.genre_selector -> {
                val fragment = GenrePickerFragment.newInstance(mGenres)
                fragment.setTargetFragment(this, REQUEST_CODE_PICK_GENRE)
                fragment.show(fragmentManager, "genre-picker")
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CODE_PICK_YEAR -> {
                val year = data.getIntExtra(NumberPickerFragment.RESULT, -1)
                Prefs.year(activity, year)
                activity.invalidateOptionsMenu()
                onYearChanged(year)
            }
            REQUEST_CODE_PICK_GENRE -> {
                val genres = data.getIntArrayExtra(GenrePickerFragment.RESULT)
                mGenres.clear()
                genres.toCollection(mGenres)
                Log.i("foobar", "genres: $mGenres")
                Prefs.genres(activity, mGenres)
                reloadData()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onMovieSelected(movieObservable: Observable<Movie>) {
        movieObservable.subscribe { movie -> mListener!!.onMovieSelected(movie) }
    }

    private fun onYearChanged(year: Int) {
        if (mSelectedYear != year) {
            mSelectedYear = year
            reloadData()
        }
    }

    private fun reloadData() {
        (view.findViewById(R.id.movies_list) as RecyclerView).let {
            val app = activity.applicationContext as MyApplication
            it.adapter = MoviesAdapter(app, mSelectedYear, mGenres, this)
        }
    }
}

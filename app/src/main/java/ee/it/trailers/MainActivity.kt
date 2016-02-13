package ee.it.trailers

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import ee.it.trailers.tmdb.Movie

class MainActivity : AppCompatActivity(), MoviesFragment.OnMovieSelected {
    private val toolbar: Toolbar by lazy { findViewById(R.id.my_toolbar) as Toolbar }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, MoviesFragment())
                    .commit()
        }
    }

    override fun onBackPressed() {
        val fm = fragmentManager
        val count = fm.backStackEntryCount
        if (count > 0) {
            if (count == 1) {
                toolbar.setTitle(R.string.app_name)
            }

            fm.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMovieSelected(movie: Movie) {
        toolbar.title = movie.title

        val fragment = MovieDetailsFragment.newInstance(movie)
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
    }
}

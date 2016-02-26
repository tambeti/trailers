package ee.it.trailers

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MoviesFragment.OnMovieSelected {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(R.layout.activity_main)

        setSupportActionBar(my_toolbar)

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
                my_toolbar.setTitle(R.string.app_name)
            }

            fm.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMovieSelected(movie: Movie) {
        my_toolbar.title = movie.title

        val fragment = MovieDetailsFragment.newInstance(movie)
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
    }
}

package ee.it.trailers;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import ee.it.trailers.tmdb.Movie;

public class MainActivity extends AppCompatActivity implements MoviesFragment.OnMovieSelected {
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            //TrailersFragment fragment = new TrailersFragment();
            MoviesFragment fragment = new MoviesFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        final android.app.FragmentManager fm = getFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count > 0) {
            if (count == 1) {
                mToolbar.setTitle(R.string.app_name);
            }

            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMovieSelected(Movie movie) {
        mToolbar.setTitle(movie.title);

        final Fragment fragment = MovieDetailsFragment.newInstance(movie.id);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}

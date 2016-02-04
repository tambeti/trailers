package ee.it.trailers;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ee.it.trailers.tmdb.Movie;
import rx.functions.Action1;

public class MoviesFragment extends ListFragment {
    public interface OnMovieSelected {
        void onMovieSelected(Movie movie);
    }

    private static final int REQUEST_CODE_PICK_YEAR = 1;
    private static final int REQUEST_CODE_PICK_GENRE = 2;

    private OnMovieSelected mListener;
    private int mSelectedYear;
    private List<Integer> mGenres;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnMovieSelected) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnArticleSelectedListener");
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final MyAdapter adapter = (MyAdapter) listView.getAdapter();
                adapter.loadMovie(position)
                        .subscribe(new Action1<Movie>() {
                            @Override
                            public void call(Movie movie) {
                                mListener.onMovieSelected(movie);
                            }
                        });
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        final Activity activity = getActivity();
        activity.invalidateOptionsMenu();
        mGenres = Prefs.genres(activity);
        onYearChanged(Prefs.year(activity));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final MenuItem item = menu.findItem(R.id.year_selector);
        item.setTitle(Integer.toString(mSelectedYear));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.year_selector:
                final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                final DialogFragment fragment = NumberPickerFragment.newInstance(1920,
                        currentYear, mSelectedYear);
                fragment.setTargetFragment(this, REQUEST_CODE_PICK_YEAR);
                fragment.show(getFragmentManager(), "year-picker");
                break;
            case R.id.genre_selector:
                final DialogFragment gf = GenrePickerFragment.newInstance(mGenres);
                gf.setTargetFragment(this, REQUEST_CODE_PICK_GENRE);
                gf.show(getFragmentManager(), "genre-picker");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_YEAR) {
            int year = data.getIntExtra(NumberPickerFragment.RESULT, -1);
            Prefs.year(getActivity(), year);
            getActivity().invalidateOptionsMenu();
            onYearChanged(year);
        } else if (requestCode == REQUEST_CODE_PICK_GENRE) {
            int[] genres = data.getIntArrayExtra(GenrePickerFragment.RESULT);
            mGenres.clear();
            for (int g : genres) {
                mGenres.add(g);
            }
            Prefs.genres(getActivity(), mGenres);
            reloadData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onYearChanged(int year) {
        if (mSelectedYear != year) {
            mSelectedYear = year;
            reloadData();
        }
    }

    private void reloadData() {
        final MyApplication app = (MyApplication) getActivity().getApplicationContext();
        final MyAdapter adapter = new MyAdapter(app, mSelectedYear, mGenres);
        setListAdapter(adapter);
    }
}

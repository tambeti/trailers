package ee.it.trailers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ee.it.trailers.tmdb.Genres;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class GenrePickerFragment extends DialogFragment {
    public static final String RESULT = "result";
    private static final String ARG_INITIAL_VALUES = "arg_initial_values";
    private List<CheckBox> mCheckBoxes = new ArrayList<>();

    public static GenrePickerFragment newInstance(List<Integer> initialValues) {
        final Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_INITIAL_VALUES, new ArrayList<>(initialValues));

        final GenrePickerFragment fragment = new GenrePickerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        final Set<Integer> initial = new HashSet<>(getArguments().getIntegerArrayList(ARG_INITIAL_VALUES));

        final LinearLayout genreList = genreSelector(activity);
        final ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(genreList);

        final MyApplication app = (MyApplication) activity.getApplicationContext();
        app.movieGenres()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Genres>() {
                    @Override
                    public void call(Genres genres) {
                        for (Genres.Genre g : genres.genres) {
                            final CheckBox v = new CheckBox(activity);
                            v.setText(g.name);
                            v.setTag(g);
                            if (initial.contains(g.id)) {
                                v.setChecked(true);
                            }
                            genreList.addView(v);
                            mCheckBoxes.add(v);
                        }
                    }
                });

        return new AlertDialog.Builder(getActivity())
                .setTitle("Choose genres")
                .setView(scrollView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final List<Integer> list = new ArrayList<>();
                        for (CheckBox cb : mCheckBoxes) {
                            if (cb.isChecked()) {
                                final Genres.Genre g = (Genres.Genre) cb.getTag();
                                list.add(g.id);
                            }
                        }

                        sendResults(list);
                    }
                })
                .setNeutralButton("All", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResults(Collections.<Integer>emptyList());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
    }

    private LinearLayout genreSelector(Context context) {
        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);



        return layout;
    }

    private void sendResults(List<Integer> results) {
        final Intent intent = new Intent();
        int[] array = new int[results.size()];
        int i = 0;
        for (int r : results) {
            array[i++] = r;
        }
        intent.putExtra(RESULT, array);
        getTargetFragment().onActivityResult(getTargetRequestCode(),
                Activity.RESULT_OK, intent);
    }
}

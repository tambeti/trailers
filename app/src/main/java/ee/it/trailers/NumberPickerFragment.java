package ee.it.trailers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.NumberPicker;

public class NumberPickerFragment extends DialogFragment {
    public static final String RESULT = "result";
    private static final String ARG_MIN = "arg_min";
    private static final String ARG_MAX = "arg_max";
    private static final String ARG_INITIAL = "arg_selected";

    public static NumberPickerFragment newInstance(int from, int to, int initial) {
        final Bundle args = new Bundle();
        args.putInt(ARG_MIN, from);
        args.putInt(ARG_MAX, to);
        args.putInt(ARG_INITIAL, initial);

        NumberPickerFragment fragment = new NumberPickerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final NumberPicker picker = new NumberPicker(getActivity());
        final Bundle args = getArguments();
        picker.setMinValue(args.getInt(ARG_MIN));
        picker.setMaxValue(args.getInt(ARG_MAX));
        picker.setValue(args.getInt(ARG_INITIAL));

        return new AlertDialog.Builder(getActivity())
                .setView(picker)
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("foobar", "picked " + picker.getValue());
                        final Intent intent = new Intent();
                        intent.putExtra(RESULT, picker.getValue());
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, intent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
    }
}

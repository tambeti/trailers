package ee.it.trailers

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.NumberPicker

class NumberPickerFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val picker = NumberPicker(activity).apply {
            val args = arguments
            minValue = args.getInt(ARG_MIN)
            maxValue = args.getInt(ARG_MAX)
            value = args.getInt(ARG_INITIAL)
        }

        return AlertDialog.Builder(activity)
                .setView(picker)
                .setPositiveButton("ok") { dialog, which ->
                    Log.i("foobar", "picked " + picker.value)
                    val intent = Intent()
                    intent.putExtra(RESULT, picker.value)
                    targetFragment.onActivityResult(targetRequestCode,
                            Activity.RESULT_OK, intent)
                }
                .setNegativeButton("Cancel", object: DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {}
                })
                .create()
    }

    companion object {
        val RESULT = "result"
        private val ARG_MIN = "arg_min"
        private val ARG_MAX = "arg_max"
        private val ARG_INITIAL = "arg_selected"

        fun newInstance(from: Int, to: Int, initial: Int): NumberPickerFragment {
            return NumberPickerFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_MIN, from)
                    putInt(ARG_MAX, to)
                    putInt(ARG_INITIAL, initial)
                }
            }
        }
    }
}

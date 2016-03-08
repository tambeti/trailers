package ee.it.trailers

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class GenrePickerFragment : DialogFragment() {
    private val checkBoxes: MutableList<CheckBox> = mutableListOf()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val initial = arguments?.getIntegerArrayList(ARG_INITIAL_VALUES)
                ?.filter { it != null }
                ?.map { it!! }
                ?.toSet()
                ?: throw IllegalArgumentException("no initial values")

        val genreList = genreSelector(activity)
        val scrollView = ScrollView(activity)
        scrollView.addView(genreList)

        val app = activity.applicationContext as MyApplication
        app.genres
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { genres ->
                    for (g in genres.genres) {
                        val v = CheckBox(activity)
                        v.text = g.name
                        v.tag = g.id
                        if (initial.contains(g.id)) {
                            v.isChecked = true
                        }
                        genreList.addView(v)
                        checkBoxes.add(v)
                    }
                }

        return AlertDialog.Builder(activity)
                .setTitle("Choose genres")
                .setView(scrollView)
                .setPositiveButton("OK") { dialog, which ->
                    val list = checkBoxes.filter { it.isChecked }
                            .map { it.tag as Int }
                    sendResults(list)
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("All") { dialog, which ->
                    sendResults(emptyList<Int>())
                }
                .create()
    }

    private fun genreSelector(context: Context): LinearLayout {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        return layout
    }

    private fun sendResults(results: List<Int>) {
        val intent = Intent()
        intent.putExtra(RESULT, results.toIntArray())
        targetFragment.onActivityResult(targetRequestCode,
                Activity.RESULT_OK, intent)
    }

    companion object {
        val RESULT = "result"
        private val ARG_INITIAL_VALUES = "arg_initial_values"

        fun newInstance(initialValues: List<Int>): GenrePickerFragment {
            val args = Bundle()
            args.putIntegerArrayList(ARG_INITIAL_VALUES, ArrayList(initialValues))

            val fragment = GenrePickerFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

package ee.it.trailers

import android.app.Activity
import android.app.DialogFragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.squareup.okhttp.Request
import ee.it.trailers.tmdb.Movie
import kotlinx.android.synthetic.main.torrent_picker.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions
import java.io.IOException
import java.util.concurrent.TimeUnit

class TorrentPickerFragment : DialogFragment() {
    enum class Action { PLAY, COPY }

    interface OnTorrentSelectedListener {
        fun onTorrentSelected(torrent: Torrent, action: Action)
    }

    private val sub: SerialSubscription = SerialSubscription()

    private val httpClient by lazy {
        (activity.applicationContext as MyApplication).httpClient
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater?.inflate(R.layout.torrent_picker, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        torrent_list.layoutManager = LinearLayoutManager(view!!.context)
    }

    class RetryWithDelay(val maxRetries: Int, val retryDelayMs: Long)
    : Func1<Observable<out Throwable>, Observable<*>> {
        var retries = 0

        override fun call(attempts: Observable<out Throwable>?): Observable<*>? {
            return attempts?.flatMap { throwable ->
                if (++retries < maxRetries) {
                    Observable.timer(retryDelayMs, TimeUnit.MILLISECONDS)
                } else {
                    Observable.error(throwable as Throwable)
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()

        val listener: OnTorrentSelectedListener = object: OnTorrentSelectedListener {
            override fun onTorrentSelected(torrent: Torrent, action: Action) {
                val intent = Intent().apply {
                    putExtra(KEY_NAME, torrent.name)
                    putExtra(KEY_MAGNET, torrent.magnet)
                    putExtra(KEY_ACTION, action)
                }
                targetFragment?.onActivityResult(targetRequestCode, Activity.RESULT_OK, intent)
                dismiss()
            }
        }

        (arguments?.getSerializable(KEY_MOVIE) as Movie?)?.let { movie ->
            val s = search(movie)
                    .toList()
                    .retryWhen(RetryWithDelay(5, 2000))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ torrents: List<Torrent> ->
                        torrent_list.adapter = TorrentPickerAdapter(torrents, listener)
                    }, { t: Throwable ->
                        dismiss()
                        Toast.makeText(activity, t.message, Toast.LENGTH_SHORT)
                                .show()
                    })
            sub.set(s)
        }
    }

    override fun onStop() {
        super.onStop()
        sub.set(Subscriptions.empty())
    }

    private fun search(movie: Movie): Observable<Torrent> {
        val url = TpbsearchUrl(movie)
        Log.i("foobar", "url: $url")

        val request = Request.Builder()
                .url(url)
                .build()

        return Observable.create { subscriber ->
            try {
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body().use {
                        Tpbparse(it.byteStream(), url)
                                .takeWhile { !subscriber.isUnsubscribed }
                                .forEach { subscriber.onNext(it) }
                    }

                    subscriber.onCompleted()
                } else {
                    throw IOException("Unsuccessful response: ${response.code()}")
                }
            } catch (e: IOException) {
                subscriber.onError(e)
            }
        }
    }


    companion object {
        val KEY_NAME = "torrent-picker-name"
        val KEY_MAGNET = "torrent-picker-magnet"
        val KEY_ACTION = "torrent-picker-action"
        private val KEY_MOVIE = "torrent-picker-movie"

        fun newInstance(movie: Movie) = TorrentPickerFragment().apply {
            arguments = Bundle().apply { putSerializable(KEY_MOVIE, movie) }
        }
    }
}
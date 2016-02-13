package ee.it.trailers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.okhttp.Callback
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

class Pulsar(private val mContext: Context, httpClient: OkHttpClient) {
    private val mHttpClient: OkHttpClient
    private val mBaseUrl: Uri

    init {
        mHttpClient = httpClient.clone()
        mHttpClient.setReadTimeout(2, TimeUnit.MINUTES)
        mHttpClient.followRedirects = false
        mBaseUrl = Uri.parse("http://" + Prefs.kodiIP(mContext) + ":" + PULSAR_PORT)
    }

    fun trailerUrl(source: String): String {
        val uri = mBaseUrl.buildUpon().appendPath("youtube").appendPath(source).build()
        return uri.toString()
    }

    fun play(imdbId: String) {
        val uri = mBaseUrl.buildUpon().appendPath("movie").appendPath(imdbId).appendPath("play").build()
        val request = Request.Builder().url(uri.toString()).build()

        mHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                Log.w(TAG, "Quasar play failed:", e)
            }

            @Throws(IOException::class)
            override fun onResponse(response: Response) {
                val body = response.body().string()
                Log.i(TAG, "play response: " + body)
                playTorrent(body)
            }
        })
    }

    private fun playTorrent(response: String) {
        val link = extractLink(response) ?: return
        val url = link.replace("plugin://plugin.video.quasar", mBaseUrl.toString())
        Log.i(TAG, "url: " + url)
        val request = Request.Builder().url(url).build()

        mHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                Log.w(TAG, "Pulsar play2 failed:", e)
            }

            @Throws(IOException::class)
            override fun onResponse(response: Response) {
                val body = response.body().string()
                Log.i(TAG, "play2 response: " + body)
                val link = extractLink(body)
                if (link != null) {
                    Kodi.play(mContext, mHttpClient, link)
                }
            }
        })
    }

    companion object {
        private val TAG = Pulsar::class.java.simpleName
        private val PULSAR_PORT = 65251

        private fun extractLink(response: String): String? {
            val doc = Jsoup.parse(response, "")
            val links = doc.select("a[href]")
            if (!links.isEmpty()) {
                return links.first().attr("href")
            } else {
                Log.w(TAG, "No links found")
                return null
            }
        }
    }
}

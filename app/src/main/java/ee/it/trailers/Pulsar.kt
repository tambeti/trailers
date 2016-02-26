    package ee.it.trailers

    import android.content.Context
    import android.net.Uri
    import android.util.Log
    import com.squareup.okhttp.OkHttpClient
    import org.jsoup.Jsoup
    import rx.schedulers.Schedulers
    import java.util.concurrent.TimeUnit

    class Pulsar(private val context: Context, httpClient: OkHttpClient) {
        private val httpClient: OkHttpClient
        private val baseUrl: Uri

        init {
            this.httpClient = httpClient.clone().apply {
                setReadTimeout(2, TimeUnit.MINUTES)
                followRedirects = false
            }
            baseUrl = Uri.parse("http://${Prefs.kodiIP(context)}:$PULSAR_PORT")
        }

        fun trailerUrl(source: String) = baseUrl.buildUpon()
                .appendPath("youtube")
                .appendPath(source)
                .build()
                .toString()

        fun playUri(uri: String) {
            val url = baseUrl.buildUpon()
                    .appendPath("play")
                    .appendQueryParameter("uri", uri)
                    .build()

            httpClient.get(url.toString())
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                val body = response.body().string()
                Log.i(TAG, "play response: " + body)
                val link = extractLink(body)
                if (link != null) {
                    Kodi.play(context, httpClient, link)
                }
            }, { e ->
                Log.w(TAG, "Quasar play failed:", e)
            })
        }

        fun play(imdbId: String) {
            val uri = baseUrl.buildUpon()
                    .appendPath("movie")
                    .appendPath(imdbId)
                    .appendPath("play")
                    .build()

            httpClient.get(uri.toString())
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                val body = response.body().string()
                Log.i(TAG, "play response: " + body)
                playTorrent(body)
            }, { e ->
                Log.w(TAG, "Quasar play failed:", e)
            })
        }

        private fun playTorrent(response: String) {
            val url = extractLink(response)
                    ?.replace("plugin://plugin.video.quasar", baseUrl.toString())
                    ?: null

            Log.i(TAG, "url: " + url)
            if (url == null) {
                Log.w(TAG, "Could not extract link from $response")
                return
            }

            httpClient.get(url)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ response ->
                val body = response.body().string()
                Log.i(TAG, "play2 response: " + body)
                val link = extractLink(body)
                if (link != null) {
                    Kodi.play(context, httpClient, link)
                }
            }, { e ->
                Log.w(TAG, "Pulsar play2 failed:", e)
            })
        }

        companion object {
            private val TAG = Pulsar::class.java.simpleName
            private val PULSAR_PORT = 65251

            private fun extractLink(response: String) = Jsoup.parse(response, "")
                        ?.select("a[href]")
                        ?.first()
                        ?.attr("href")
                        ?: null
        }
    }

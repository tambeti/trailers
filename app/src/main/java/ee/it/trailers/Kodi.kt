package ee.it.trailers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.okhttp.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import rx.schedulers.Schedulers

object Kodi {
    internal fun play(context: Context, httpClient: OkHttpClient, url: String) {
        val json: String
        try {
            val item = JSONObject().put("file", Uri.encode(url))
            val params = JSONObject().put("item", item)
            val obj = JSONObject().put("jsonrpc", "2.0")
                    .put("id", 1)
                    .put("method", "Player.Open")
                    .put("params", params)

            json = obj.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
            return
        }

        //        final HttpUrl kodiUrl = new HttpUrl.Builder()
        //                .scheme("http")
        //                .host(Prefs.kodiIP(context))
        //                .port(Prefs.kodiPort(context))
        //                .addPathSegment("jsonrpc")
        //                .addEncodedQueryParameter("request", json)
        //                .build();
        //
        //        Log.i("foobar", "play kodi url: " + kodiUrl.toString());
        val kodiUrl = "http://${Prefs.kodiUrl(context)}/jsonrpc?request=$json"
        Log.i("foobar", "play kodi url: " + kodiUrl)

        httpClient.get(kodiUrl)
                .subscribeOn(Schedulers.io())
                .subscribe({ response ->
            val body = response.body().string()
            Log.i("foobar", "body: " + body)
        }, { e ->
            Log.w("foobar", "http call failed", e)
        })
    }
}

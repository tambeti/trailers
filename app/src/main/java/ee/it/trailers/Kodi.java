package ee.it.trailers;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Kodi {
    static void play(Context context, OkHttpClient httpClient, String url) {
        final String json;
        try {
            JSONObject item = new JSONObject()
                    .put("file", Uri.encode(url));

            JSONObject params = new JSONObject()
                    .put("item", item);

            JSONObject obj = new JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", 1)
                    .put("method", "Player.Open")
                    .put("params", params);

            json = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
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
        final String kodiUrl = "http://" + Prefs.kodiUrl(context) + "/jsonrpc?request=" + json;
        Log.i("foobar", "play kodi url: " + kodiUrl);

        final Request request = new Request.Builder()
                .url(kodiUrl)
                .build();

        httpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.w("foobar", "http call failed", e);
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        final String body = response.body().string();
                        Log.i("foobar", "body: " + body);
                    }
                });
    }
}

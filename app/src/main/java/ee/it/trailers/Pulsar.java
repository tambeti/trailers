package ee.it.trailers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Pulsar {
    private static final String TAG = Pulsar.class.getSimpleName();
    private static final int PULSAR_PORT = 65251;
    private final Context mContext;
    private final OkHttpClient mHttpClient;
    private final Uri mBaseUrl;

    public Pulsar(Context context, OkHttpClient httpClient) {
        mContext = context;
        mHttpClient = httpClient.clone();
        mHttpClient.setReadTimeout(2, TimeUnit.MINUTES);
        mHttpClient.setFollowRedirects(false);
        mBaseUrl = Uri.parse("http://" + Prefs.kodiIP(context) + ":" + PULSAR_PORT);
    }

    public String trailerUrl(String source) {
        final Uri uri = mBaseUrl.buildUpon()
                .appendPath("youtube")
                .appendPath(source)
                .build();
        return uri.toString();
    }

    public void play(String imdbId) {
        final Uri uri = mBaseUrl.buildUpon()
                .appendPath("movie")
                .appendPath(imdbId)
                .appendPath("play")
                .build();
        final Request request = new Request.Builder()
                .url(uri.toString())
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.w(TAG, "Quasar play failed:", e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String body = response.body().string();
                Log.i(TAG, "play response: " + body);
                playTorrent(body);
            }
        });
    }

    private void playTorrent(String response) {
        final String link = extractLink(response);
        if (link == null) {
            return;
        }
        String url = link.replace("plugin://plugin.video.quasar", mBaseUrl.toString());
        Log.i(TAG, "url: " + url);
        final Request request = new Request.Builder()
                .url(url)
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.w(TAG, "Pulsar play2 failed:", e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final String body = response.body().string();
                Log.i(TAG, "play2 response: " + body);
                final String link = extractLink(body);
                if (link != null) {
                    Kodi.play(mContext, mHttpClient, link);
                }
            }
        });
    }

    @Nullable
    private static String extractLink(String response) {
        Document doc = Jsoup.parse(response, "");
        Elements links = doc.select("a[href]");
        if (!links.isEmpty()) {
            return links.first().attr("href");
        } else {
            Log.w(TAG, "No links found");
            return null;
        }
    }
}

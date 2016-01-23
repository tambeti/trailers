package ee.it.trailers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public void searchMovie(String title) {
        final Uri uri = mBaseUrl.buildUpon()
                .appendPath("movies")
                .appendPath("search")
                .appendQueryParameter("q", title)
                .build();

        final Request request = new Request.Builder()
                .url(uri.toString())
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.w(TAG, "Pulsar search failed:", e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                final List<Movie> results = parseMedia(response.body().string());
                Log.i(TAG, "Got " + results.size() + " results");
                for (Movie m : results) {
                    Log.i(TAG, "movie: " + m.toString());
                }

                if (!results.isEmpty()) {
                    play(results.get(0));
                }
            }
        });
    }

    public void play(Movie movie) {
        final Uri uri = mBaseUrl.buildUpon()
                .appendPath("movie")
                .appendPath(movie.id)
                .appendPath("play")
                .build();
        final Request request = new Request.Builder()
                .url(uri.toString())
                .build();

        mHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                Log.w(TAG, "Pulsar play failed:", e);
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
        String url = link.replace("plugin://plugin.video.pulsar", mBaseUrl.toString());
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

    private static List<Movie> parseMedia(String body) {
        Log.i(TAG, "Search results: " + body);
        final List<Movie> list = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(body);
            JSONArray movies = obj.getJSONArray("items");
            for (int i = 0; i < movies.length(); i++) {
                try {
                    final Movie movie = parseMedia(movies.getJSONObject(i));
                    if (movie != null) {
                        list.add(movie);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Nullable
    private static Movie parseMedia(JSONObject obj) throws JSONException {
        Movie m = new Movie();
        m.title = obj.getString("label");
        m.posterUrl = obj.getString("thumbnail");
        JSONObject info = obj.getJSONObject("info");
        m.id = info.getString("code");
        m.releaseDate = info.getString("date");

        m.genres = Arrays.asList(info.optString("genre", "")
                .split(" / "));

        m.directors = Arrays.asList(info.optString("director", "")
                .split(" / "));

        m.actors = new ArrayList<>();
        JSONArray actors = info.optJSONArray("castandrole");
        if (actors != null) {
            for (int i = 0; i < actors.length(); i++) {
                JSONArray actor = actors.getJSONArray(i);
                m.actors.add(actor.getString(0));
            }
        }

        return m;
    }
}

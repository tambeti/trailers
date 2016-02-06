package ee.it.trailers;

import android.app.Application;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import ee.it.trailers.tmdb.Genres;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class MyApplication extends Application {
    public static final String POSTERS_URL = "http://image.tmdb.org/t/p/w500";
    private static final HttpUrl API_URL = HttpUrl.parse("https://api.themoviedb.org/3");
    private static final String TMDB_API_KEY = "5d93c9cc67db0baee560b7eccc07c08f";
    private OkHttpClient mHttpClient;
    private Picasso mPicasso;
    private Gson mGson;
    private Observable<Genres> mGenres;

    @Override
    public void onCreate() {
        super.onCreate();

        mHttpClient = new OkHttpClient();
        mPicasso = new Picasso.Builder(this)
                .downloader(new OkHttpDownloader(mHttpClient))
                .build();

        mGson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();

        mGenres = movieGenres(mHttpClient, mGson)
                .subscribeOn(Schedulers.io())
                .cache(1);
    }

    public OkHttpClient httpClient() {
        return mHttpClient;
    }

    public Picasso picasso() {
        return mPicasso;
    }

    public Gson gson() {
        return mGson;
    }

    public Observable<Genres> movieGenres() {
        return mGenres;
    }

    public static HttpUrl.Builder apiUrlBuilder() {
        return API_URL.newBuilder()
                .addQueryParameter("api_key", TMDB_API_KEY);
    }

    private static Observable<Genres> movieGenres(final OkHttpClient httpClient, final Gson gson) {
        return Observable.create(new Observable.OnSubscribe<Genres>() {
            @Override
            public void call(Subscriber<? super Genres> subscriber) {
                Log.i("foobar", "Fetching movie genres");
                final HttpUrl url = MyApplication.apiUrlBuilder()
                        .addPathSegment("genre")
                        .addPathSegment("movie")
                        .addPathSegment("list")
                        .build();

                final Request request = new Request.Builder()
                        .url(url)
                        .build();

                try {
                    final Response response = httpClient.newCall(request).execute();
                    final String body = response.body().string();
                    final Genres genres = gson.fromJson(body, Genres.class);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(genres);
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }
}

package ee.it.trailers;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import ee.it.trailers.tmdb.MovieDetails;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class MovieDetailsFragment extends Fragment {
    private static final String KEY_ID = "movie-details-id";
    private OkHttpClient mHttpClient;
    private Gson mGson;
    private Picasso mPicasso;
    private Pulsar mPulsar;
    private long mId;

    public static MovieDetailsFragment newInstance(long movieId) {
        final Bundle args = new Bundle();
        args.putLong(KEY_ID, movieId);

        final MovieDetailsFragment fragment = new MovieDetailsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final MyApplication app = (MyApplication) getActivity().getApplicationContext();
        mHttpClient = app.httpClient();
        mGson = app.gson();
        mPicasso = app.picasso();
        mPulsar = new Pulsar(app, mHttpClient);

        mId = getArguments().getLong(KEY_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ImageView poster = (ImageView) view.findViewById(R.id.poster);
        final TextView genre = (TextView) view.findViewById(R.id.genres);
        final TextView runtime = (TextView) view.findViewById(R.id.runtime);
        final TextView director = (TextView) view.findViewById(R.id.director);
        final TextView writer = (TextView) view.findViewById(R.id.writer);
        final TextView cast = (TextView) view.findViewById(R.id.cast);

        final TextView plot = (TextView) view.findViewById(R.id.plot);
        final Button trailer = (Button) view.findViewById(R.id.trailer);
        final Button play = (Button) view.findViewById(R.id.play);

        trailer.setEnabled(false);

        movieDetails(mId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MovieDetails>() {
                    @Override
                    public void call(final MovieDetails movie) {
                        plot.setText(movie.overview);
                        runtime.setText(Long.toString(movie.runtime) + " minutes");
                        genre.setText(movie.genres());

                        director.setText(movie.directorNames());
                        writer.setText(movie.writerNames());
                        cast.setText(movie.castNames(10));

                        final String trailerSource = movie.youtubeTrailer();
                        if (trailerSource != null) {
                            final String url = mPulsar.trailerUrl(trailerSource);
                            trailer.setEnabled(true);
                            trailer.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Kodi.play(getActivity(), mHttpClient, url);
                                }
                            });
                        }

                        play.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mPulsar.play(movie.imdbId);
                            }
                        });

                        mPicasso.load(MyApplication.POSTERS_URL + movie.posterPath)
                                .into(poster);
                    }
                });
    }

    private Observable<MovieDetails> movieDetails(final long id) {
        return Observable.create(new Observable.OnSubscribe<MovieDetails>() {
            @Override
            public void call(Subscriber<? super MovieDetails> subscriber) {
                final HttpUrl url = MyApplication.apiUrlBuilder()
                        .addPathSegment("movie")
                        .addPathSegment(Long.toString(id))
                        .addQueryParameter("append_to_response", "trailers,credits")
                        .build();

                final Request request = new Request.Builder()
                        .url(url)
                        .build();

                try {
                    final Response response = mHttpClient.newCall(request).execute();
                    final String body = response.body().string();
                    final MovieDetails movie = mGson.fromJson(body, MovieDetails.class);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(movie);
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

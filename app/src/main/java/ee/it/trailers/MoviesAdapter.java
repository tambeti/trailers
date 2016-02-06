package ee.it.trailers;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ee.it.trailers.tmdb.DiscoverResult;
import ee.it.trailers.tmdb.Genres;
import ee.it.trailers.tmdb.Movie;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.ViewHolder> {
    public interface OnMovieSelectedListener {
        void onMovieSelected(Observable<Movie> movieObservable);
    }

    private static final int PAGE_SIZE = 20;
    private final OkHttpClient mHttpClient;
    private final Gson mGson;
    private final Picasso mPicasso;
    private final int mYear;
    private final List<Integer> mGenreFilter;
    private int mCount;
    private final OnMovieSelectedListener mMovieSelectedListener;
    private final Observable<Genres> mGenres;
    private final List<Observable<List<ee.it.trailers.tmdb.Movie>>> mObservables = new ArrayList<>();

    public MoviesAdapter(MyApplication app, int year, List<Integer> genreFilter,
                         OnMovieSelectedListener movieSelectedListener) {
        Log.i("foobar", "creating adapter");
        mHttpClient = app.httpClient();
        mGson = app.gson();
        mPicasso = app.picasso();
        mGenres = app.movieGenres();
        mYear = year;
        mGenreFilter = genreFilter;
        mMovieSelectedListener = movieSelectedListener;

        initObservables();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item,
                parent, false);
        return new ViewHolder(view, mMovieSelectedListener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bindMovie(loadMovie(position), mGenres, mPicasso);
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.reset(mPicasso);
        super.onViewRecycled(holder);
    }

    public Observable<Movie> loadMovie(int position) {
        final int page = position / PAGE_SIZE;
        final int positionInPage = position % PAGE_SIZE;

        return mObservables.get(page)
                .map(new Func1<List<Movie>, Movie>() {
                    @Override
                    public Movie call(List<Movie> movies) {
                        return movies.get(positionInPage);
                    }
                });
    }

    private void initObservables() {
        discoverMovies(mHttpClient, mGson, mYear, mGenreFilter, 0)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<DiscoverResult>() {
                    @Override
                    public void call(DiscoverResult discoverResult) {
                        mCount = discoverResult.totalResults;

                        final Observable<List<ee.it.trailers.tmdb.Movie>> page0 = Observable.just(discoverResult)
                                .map(new Func1<DiscoverResult, List<ee.it.trailers.tmdb.Movie>>() {
                                    @Override
                                    public List<ee.it.trailers.tmdb.Movie> call(DiscoverResult discoverResult) {
                                        return discoverResult.results;
                                    }
                                })
                                .cache(1);

                        mObservables.add(page0);

                        for (int page = 0; page < discoverResult.totalPages; page++) {
                            final Observable<List<ee.it.trailers.tmdb.Movie>> o = loadPage(page + 2).cache(1);
                            mObservables.add(o);
                        }

                        notifyDataSetChanged();
                    }
                });
    }

    Observable<List<ee.it.trailers.tmdb.Movie>> loadPage(int page) {
        return discoverMovies(mHttpClient, mGson, mYear, mGenreFilter, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<DiscoverResult, List<ee.it.trailers.tmdb.Movie>>() {
                    @Override
                    public List<Movie> call(DiscoverResult discoverResult) {
                        return discoverResult.results;
                    }
                });
    }

    private static Observable<DiscoverResult> discoverMovies(final OkHttpClient httpClient,
                                                             final Gson gson,
                                                             final int year,
                                                             final List<Integer> genreFilter,
                                                             final int page) {
        return Observable.create(new Observable.OnSubscribe<DiscoverResult>() {
            @Override
            public void call(Subscriber<? super DiscoverResult> subscriber) {
                Log.i("foobar", "Loading page " + page);
                final HttpUrl.Builder urlBuilder = MyApplication.apiUrlBuilder()
                        .addPathSegment("discover")
                        .addPathSegment("movie")
                        .addQueryParameter("primary_release_year", Integer.toString(year))
                        .addQueryParameter("vote_count.gte", "50")
                        .addQueryParameter("language", "en")
                        .addQueryParameter("sort_by", "primary_release_date.asc");

                if (!genreFilter.isEmpty()) {
                    final StringBuilder sb = new StringBuilder();
                    boolean addComma = false;
                    for (int g : genreFilter) {
                        if (addComma) {
                            sb.append(",");
                        } else {
                            addComma = true;
                        }

                        sb.append(g);
                    }

                    urlBuilder.addQueryParameter("with_genres", sb.toString());
                }

                if (page > 0) {
                    urlBuilder.addQueryParameter("page", Integer.toString(page));
                }

                final Request request = new Request.Builder()
                        .url(urlBuilder.build())
                        .build();

                try {
                    final Response response = httpClient.newCall(request).execute();
                    final String body = response.body().string();
                    Log.i("foobar", body);
                    final DiscoverResult result = gson.fromJson(body, DiscoverResult.class);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(result);
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

    private static class MovieWithGenres {
        final Movie movie;
        final Genres genres;

        public MovieWithGenres(Movie movie, Genres genres) {
            this.movie = movie;
            this.genres = genres;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private final SerialSubscription mSub = new SerialSubscription();
        private Observable<Movie> mObservable = Observable.empty();

        ImageView poster;
        TextView title;
        TextView genre;
        TextView releaseDate;

        public ViewHolder(final View view, final OnMovieSelectedListener listener) {
            super(view);

            poster = (ImageView) view.findViewById(R.id.poster);
            title = (TextView) view.findViewById(R.id.title);
            genre = (TextView) view.findViewById(R.id.genre);
            releaseDate = (TextView) view.findViewById(R.id.release_date);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMovieSelected(mObservable);
                    }
                }
            });
        }

        public void bindMovie(Observable<Movie> movieObservable, Observable<Genres> genresObservable,
                              final Picasso picasso) {
            mObservable = movieObservable;

            final Subscription sub = Observable.zip(genresObservable, movieObservable,
                    new Func2<Genres, Movie, MovieWithGenres>() {
                        @Override
                        public MovieWithGenres call(Genres genres, Movie movie) {
                            return new MovieWithGenres(movie, genres);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<MovieWithGenres>() {
                        @Override
                        public void call(MovieWithGenres data) {
                            bindMovie(data.movie, data.genres, picasso);
                        }
                    });

            mSub.set(sub);
        }

        public void bindMovie(final Movie movie, final Genres genres, Picasso picasso) {
            title.setText(movie.title);
            releaseDate.setText(movie.releaseDate);
            genre.setText(movie.genres(genres));

            picasso.load(MyApplication.POSTERS_URL_MINI + movie.posterPath)
                    .resize(185, 185)
                    .centerCrop()
                    .into(poster);
        }

        public void reset(Picasso picasso) {
            picasso.cancelRequest(poster);
            mSub.set(Subscriptions.empty());
            mObservable = Observable.empty();
        }
    }
}

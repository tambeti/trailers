package ee.it.trailers;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

public class MyAdapter extends BaseAdapter {
    private static final int PAGE_SIZE = 20;
    private final OkHttpClient mHttpClient;
    private final Gson mGson;
    private final Picasso mPicasso;
    private final int mYear;
    private final List<Integer> mGenreFilter;
    private int mCount;
    private final Observable<Genres> mGenres;
    private final List<Observable<List<Movie>>> mObservables = new ArrayList<>();

    public MyAdapter(MyApplication app, int year, List<Integer> genreFilter) {
        Log.i("foobar", "creating adapter");
        mHttpClient = app.httpClient();
        mGson = app.gson();
        mPicasso = app.picasso();
        mGenres = app.movieGenres();
        mYear = year;
        mGenreFilter = genreFilter;

        initObservables();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        final ViewHolder holder;
        if (convertView != null) {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        } else {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.movie_item, parent, false);

            holder = new ViewHolder();
            holder.poster = (ImageView) view.findViewById(R.id.poster);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.genre = (TextView) view.findViewById(R.id.genre);
            holder.director = (TextView) view.findViewById(R.id.director);
            holder.actors = (TextView) view.findViewById(R.id.actors);
            holder.releaseDate = (TextView) view.findViewById(R.id.release_date);

            view.setTag(holder);
        }

        holder.poster.setImageBitmap(null);
        holder.title.setText("");
        holder.genre.setText("");
        holder.releaseDate.setText("");

        final CompositeSubscription sub = new CompositeSubscription();
        holder.sub.set(sub);

        final Subscription movieSub = loadMovie(position)
                .subscribe(new Action1<Movie>() {
                    @Override
                    public void call(final Movie movie) {
                        holder.title.setText(movie.title);
                        //holder.director.setText(movie.directorsString());
                        //holder.actors.setText(movie.actorsString());
                        holder.releaseDate.setText(movie.releaseDate);

                        final Subscription genreSub = mGenres.observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<Genres>() {
                                    @Override
                                    public void call(Genres genres) {
                                        holder.genre.setText(movie.genres(genres));
                                    }
                                });

                        sub.add(genreSub);

                        mPicasso.load(MyApplication.POSTERS_URL + movie.posterPath)
                                .resize(200, 200)
                                .centerCrop()
                                .into(holder.poster);
                    }
                });

        sub.add(movieSub);

        return view;
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

                        final Observable<List<Movie>> page0 = Observable.just(discoverResult)
                                .map(new Func1<DiscoverResult, List<Movie>>() {
                                    @Override
                                    public List<Movie> call(DiscoverResult discoverResult) {
                                        return discoverResult.results;
                                    }
                                })
                                .cache(1);

                        mObservables.add(page0);

                        for (int page = 0; page < discoverResult.totalPages; page++) {
                            final Observable<List<Movie>> o = loadPage(page + 2).cache(1);
                            mObservables.add(o);
                        }

                        notifyDataSetChanged();
                    }
                });
    }

    Observable<List<Movie>> loadPage(int page) {
        return discoverMovies(mHttpClient, mGson, mYear, mGenreFilter, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<DiscoverResult, List<Movie>>() {
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

    static class ViewHolder {
        final SerialSubscription sub = new SerialSubscription();

        ImageView poster;
        TextView title;
        TextView genre;
        TextView director;
        TextView actors;
        TextView releaseDate;
    }
}

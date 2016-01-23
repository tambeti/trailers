package ee.it.trailers;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrailersFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Movie>> {
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/601.2.4 (KHTML, like Gecko) Version/9.0.1 Safari/601.2.4";
    private static final String TRAILER_FORMAT = "http://teaser-trailer.com/movies-%d.html";
    private static final String MOVIE_BASE_URL = "http://www.dailymotion.com/embed/video/";
    private static final String ARG_URL = "arg_url";
    private static final String ARG_YEAR = "arg_year";
    private static final int REQUEST_CODE_PICK_YEAR = 1;
    private OkHttpClient mHttpClient;
    private MovieAdapter mAdapter;
    private Pulsar mPulsar;
    private int mSelectedYear;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trailers, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHttpClient = createHttpClient();
        mPulsar = new Pulsar(view.getContext(), mHttpClient);
        mAdapter = new MovieAdapter();

        final ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playMovie((Movie)mAdapter.getItem(position));
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Movie movie = (Movie) mAdapter.getItem(position);
                mPulsar.searchMovie(movie.title);
                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        final Activity activity = getActivity();
        mSelectedYear = Prefs.year(activity);
        activity.invalidateOptionsMenu();

        getLoaderManager().initLoader(0, args(mSelectedYear), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final MenuItem item = menu.findItem(R.id.year_selector);
        item.setTitle(Integer.toString(mSelectedYear));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.year_selector:
                final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                final DialogFragment fragment = NumberPickerFragment.newInstance(2010,
                        currentYear, mSelectedYear);
                fragment.setTargetFragment(this, REQUEST_CODE_PICK_YEAR);
                fragment.show(getFragmentManager(), "year-picker");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_YEAR) {
            mSelectedYear = data.getIntExtra(NumberPickerFragment.RESULT, -1);
            Prefs.year(getActivity(), mSelectedYear);
            getActivity().invalidateOptionsMenu();
            getLoaderManager().restartLoader(0, args(mSelectedYear), this);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Bundle args(int year) {
        final Bundle args = new Bundle();
        args.putString(ARG_URL, String.format(Locale.US, TRAILER_FORMAT, year));
        args.putString(ARG_YEAR, Integer.toString(year));
        return args;
    }

    private void playMovie(Movie movie) {
        final String url = MOVIE_BASE_URL + movie.id;
        Log.i("foobar", "movie url " + url);

        final Request request = new Request.Builder()
                .url(url)
                .build();

        mHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        Log.w("foobar", "http call failed", e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        final String body = response.body().string();
                        Document doc = Jsoup.parse(body, "");
                        Map<Integer, String> urls = parseMovieBody(doc);
                        if (!url.isEmpty()) {
                            Kodi.play(getActivity(), mHttpClient, pickUrl(urls));
                        }
                    }
                });
    }

    private Map<Integer, String> parseMovieBody(Document doc) {
        Map<Integer, String> map = new ArrayMap<>();
        for (Element e : doc.getElementsByTag("script")) {
            final String data = e.data();
            int start = data.indexOf("document.getElementById('player')");
            if (start < 0) {
                continue;
            }

            int end = data.indexOf(";");
            final String jsonStr = data.substring(start + 35, end);
            try {
                JSONObject json = new JSONObject(jsonStr);
                final JSONObject qualities = json.getJSONObject("metadata")
                        .getJSONObject("qualities");

                final Iterator<String> iter = qualities.keys();
                while (iter.hasNext()) {
                    final String key = iter.next();
                    if (!TextUtils.equals("auto", key)) {
                        JSONArray array = qualities.getJSONArray(key);
                        if (array.length() > 0) {
                            JSONObject val = array.getJSONObject(0);
                            map.put(Integer.parseInt(key), val.getString("url"));
                        }
                    }
                }

            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        return map;
    }

    private String pickUrl(Map<Integer, String> urls) {
        int max = 0;
        String url = "";
        for (int size : urls.keySet()) {
            if (size > max) {
                max = size;
                url = urls.get(size);
            }
        }

        return url;
    }

    @Override
    public Loader<List<Movie>> onCreateLoader(int id, Bundle args) {
        final String url = args.getString(ARG_URL);
        final String year = args.getString(ARG_YEAR);
        final Loader<List<Movie>> loader = new MovieLoader(getActivity(), mHttpClient, url, year);

        loader.forceLoad();

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<Movie>> loader, List<Movie> movies) {
        mAdapter.swapData(movies);
    }

    @Override
    public void onLoaderReset(Loader<List<Movie>> loader) {
        mAdapter.swapData(Collections.<Movie>emptyList());
    }

    private OkHttpClient createHttpClient() {
        OkHttpClient client = new OkHttpClient();
        client.networkInterceptors().add(new UserAgentInterceptor(USER_AGENT));

        return client;
    }

private static class UserAgentInterceptor implements Interceptor {
        private final String mUserAgent;

        public UserAgentInterceptor(String userAgent) {
            mUserAgent = userAgent;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", mUserAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }
}

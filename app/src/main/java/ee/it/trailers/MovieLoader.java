package ee.it.trailers;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class MovieLoader extends AsyncTaskLoader<List<Movie>> {
    private OkHttpClient mHttpClient;
    private String mUrl;
    private String mYear;

    public MovieLoader(Context context, OkHttpClient httpClient,
                       String url, String year) {
        super(context);
        mHttpClient = httpClient;
        mUrl = url;
        mYear = year;
    }

    @Override
    public List<Movie> loadInBackground() {
        final Request request = new Request.Builder()
                .url(mUrl)
                .build();

        try {
            final Response response = mHttpClient.newCall(request).execute();
            final String body = response.body().string();
            return parse(body);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    private List<Movie> parse(String body) throws IOException {
        final List<Movie> movies = new ArrayList<>();
        final Document doc = Jsoup.parse(body, "");
        final Elements movieList = doc.getElementById("movielist").children();
        for (Element e : movieList) {
            for (Element c : e.children()) {
                if (TextUtils.equals("movieleft", c.className()) ||
                        TextUtils.equals("movieright", c.className())) {
                    final Movie movie = parseMovie(c);
                    if (movie != null) {
                        movies.add(movie);
                    }
                }
            }
        }

        return movies;
    }

    @Nullable
    private Movie parseMovie(Element movie) {
        Movie m = new Movie();
        final Element poster = movie.getElementsByClass("movieposter")
                .first()
                .getElementsByTag("img")
                .first();
        m.posterUrl = poster.attr("src");

        final Element desc = movie.getElementsByClass("moviedesc")
                .first();

        for (Element e : desc.getElementsByTag("strong")) {
            switch (e.text()) {
                case "Movie Title:":
                    final Element title = e.nextElementSibling();
                    if (title != null) {
                        m.title = title.text().trim();
                    }
                    break;
                case "Genre:":
                    final String genre = e.nextSibling()
                            .toString()
                            .trim();
                    m.genres = Arrays.asList(genre.split(", "));
                    break;
                case "Director:":
                    final String director = e.nextSibling()
                            .toString()
                            .trim();
                    m.directors = Arrays.asList(director.split(", "));
                    break;
                case "Release Date:":
                    m.releaseDate = e.nextSibling().toString().trim() + " " + mYear;
                    break;
                case "Starring:":
                    final Node content = e.nextSibling()
                            .nextSibling();

                    if (content instanceof TextNode) {
                        final String txt = ((TextNode) content).text().trim();
                        m.actors = Arrays.asList(txt.split(", "));
                    }
                    break;
                default:
                    Log.i("foobar", "desc " + e.toString());
            }
        }

        final Element trailer = desc.select("a[onClick]")
                .first();
        if (trailer != null) {
            final String s = trailer.attr("onClick");
            int start = s.indexOf("'") + 1;
            int end = s.indexOf("'", start);
            m.id = s.substring(start, end);
        }

        if (!TextUtils.isEmpty(m.id)) {
            return m;
        } else {
            return null;
        }
    }
}

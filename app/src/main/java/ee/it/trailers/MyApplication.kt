package ee.it.trailers

import android.app.Application
import android.util.JsonReader
import android.util.Log
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.picasso.OkHttpDownloader
import com.squareup.picasso.Picasso
import ee.it.trailers.tmdb.Genres
import rx.Observable
import rx.schedulers.Schedulers
import java.io.IOException

class MyApplication : Application() {
    val httpClient = OkHttpClient()
    val picasso by lazy {
        Picasso.Builder(this)
                .downloader(OkHttpDownloader(httpClient))
                .build()
    }

    val genres by lazy {
        movieGenres()
                .subscribeOn(Schedulers.io())
                .cache(1)
    }

    private fun movieGenres(): Observable<Genres> {
        return Observable.create { subscriber ->
            Log.i("foobar", "Fetching movie genres")
            val url = MyApplication.apiUrlBuilder()
                    .addPathSegment("genre")
                    .addPathSegment("movie")
                    .addPathSegment("list")
                    .build()

            val request = Request.Builder()
                    .url(url)
                    .build()

            try {
                val response = httpClient.newCall(request).execute()
                val genres = JsonReader(response.body().charStream()).use {
                    Genres.Builder(it).build()
                }
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(genres)
                    subscriber.onCompleted()
                }
            } catch (e: IOException) {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onError(e)
                }
            }
        }
    }

    companion object {
        val POSTERS_URL = "http://image.tmdb.org/t/p/w500"
        val POSTERS_URL_MINI = "http://image.tmdb.org/t/p/w185"
        private val API_URL = HttpUrl.parse("https://api.themoviedb.org/3")
        private val TMDB_API_KEY = "5d93c9cc67db0baee560b7eccc07c08f"

        fun apiUrlBuilder(): HttpUrl.Builder {
            return API_URL.newBuilder().addQueryParameter("api_key", TMDB_API_KEY)
        }
    }
}

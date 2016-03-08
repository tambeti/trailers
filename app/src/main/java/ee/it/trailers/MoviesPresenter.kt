package ee.it.trailers

import android.content.Context
import android.util.JsonReader
import android.util.Log
import com.squareup.okhttp.OkHttpClient
import ee.it.trailers.tmdb.DiscoverResult
import ee.it.trailers.tmdb.Movie
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

private val PAGE_SIZE = 20

class MoviesData(val count: Int, val observables: List<Observable<List<Movie>>>) {
    fun loadMovie(position: Int): Observable<Movie> {
        val page = position / PAGE_SIZE
        val positionInPage = position % PAGE_SIZE

        return observables[page].map { movies -> movies[positionInPage] }
    }

    companion object {
        val EMPTY: MoviesData = MoviesData(0, emptyList())
    }
}

class MoviesPresenter(val context: Context): Presenter<MoviesView>, MoviesAdapter.OnMovieSelectedListener {
    private var view: MoviesView? = null
    private var data: MoviesData? = null
    private val dataSub: Subscription
    private var year: Int = 0
    private val yearSub: Subscription

    init {
        Log.i("foobar", "Creating new MoviesPresenter: $this")
        val app = context.applicationContext as MyApplication
        val httpClient = app.httpClient

        dataSub = yearAndGeneres()
                .flatMap {
                    val (year, genres) = it
                    load(httpClient, year, genres)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    data = it
                    updateData()
                }

        yearSub = Prefs.yearO(context)
                .subscribe {
                    year = it
                    updateYear()
                }
    }

    override fun bindView(view: MoviesView) {
        Log.i("foobar", "bindView $view")
        this.view = view
        updateData()
        updateYear()
    }

    override fun unbindView() {
        Log.i("foobar", "unbindView")
        view = null
    }

    override fun destroy() {
        Log.i("foobar", "destroy ")
        dataSub.unsubscribe()
    }

    override fun onMovieSelected(movieObservable: Observable<Movie>) {
        movieObservable.subscribe { movie -> view?.showMovieDetails(movie) }
    }

    fun onPreferencesClicked() {
        view?.showPreferences()
    }

    fun onYearClicked() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        view?.showYearPicker(1920, currentYear, year)
    }

    fun setYear(year: Int) {
        Prefs.year(context, year)
    }

    fun onGenresClicked() {
        view?.showGenrePicker(Prefs.genres(context))
    }

    fun setGenres(genres: List<Int>) {
        Prefs.genres(context, genres)
    }

    private fun updateData() {
        view?.let { v ->
            data?.let { d ->
                v.updateMovies(d)
            }
        }
    }

    private fun updateYear() {
        view?.let { v ->
            if (year > 0) {
                v.updateYear(year.toString())
            }
        }
    }

    private fun yearAndGeneres() = Observable.combineLatest(Prefs.yearO(context),
            Prefs.generesO(context)) { year, genres -> year to genres }

    private fun load(httpClient: OkHttpClient, year: Int, genres: Set<Int>): Observable<MoviesData> {
        return discoverMovies(httpClient, year, genres)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { discoverResult ->
                    val count = discoverResult.totalResults
                    val observables: MutableList<Observable<List<Movie>>> = mutableListOf()

                    val page0 = Observable.just(discoverResult)
                            .map { discoverResult -> discoverResult.results }
                            .cache(1)

                    observables.add(page0)

                    for (page in 0 until discoverResult.totalPages) {
                        val o = loadPage(httpClient, year, genres, page + 2).cache(1)
                        observables.add(o)
                    }

                    MoviesData(count, observables)
                }
    }

    private fun loadPage(httpClient: OkHttpClient, year: Int, genres: Set<Int>, page: Int): Observable<List<Movie>> {
        return discoverMovies(httpClient, year, genres, page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { discoverResult -> discoverResult.results }
    }

    private fun discoverMovies(httpClient: OkHttpClient, year: Int, genres: Set<Int>, page: Int = 0)
            : Observable<DiscoverResult> {
        val urlBuilder = MyApplication.apiUrlBuilder()
                .addPathSegment("discover")
                .addPathSegment("movie")
                .addQueryParameter("primary_release_year", Integer.toString(year))
                .addQueryParameter("vote_count.gte", "50")
                .addQueryParameter("language", "en")
                .addQueryParameter("sort_by", "primary_release_date.asc")

        if (!genres.isEmpty()) {
            urlBuilder.addQueryParameter("with_genres", genres.joinToString(","))
        }

        if (page > 0) {
            urlBuilder.addQueryParameter("page", Integer.toString(page))
        }

        return httpClient.get(urlBuilder.build().toString())
                .map { response ->
                    JsonReader(response.body().charStream()).use {
                        DiscoverResult.Builder(it).build()
                    }
                }
                .toObservable()
    }
}
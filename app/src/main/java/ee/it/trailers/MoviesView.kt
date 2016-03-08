package ee.it.trailers

import ee.it.trailers.tmdb.Movie

interface MoviesView {
    fun updateMovies(data: MoviesData)
    fun updateYear(year: String)

    fun showMovieDetails(movie: Movie)
    fun showPreferences()
    fun showYearPicker(from: Int, to: Int, selected: Int)
    fun showGenrePicker(selected: List<Int>)
}
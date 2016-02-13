package ee.it.trailers.tmdb

import android.util.JsonReader

class DiscoverResult(val page: Int, val totalPages: Int, val totalResults: Int,
                     val results: List<Movie>) {

    class Builder(reader: JsonReader) {
        var page: Int = 0
        var totalPages: Int = 0
        var totalResults: Int = 0
        var results: MutableList<Movie> = mutableListOf()

        init {
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "page" -> page = nextInt()
                        "total_pages" -> totalPages = nextInt()
                        "total_results" -> totalResults = nextInt()
                        "results" -> parseResults(reader)
                        else -> skipValue()
                    }
                }
                endObject()
            }
        }

        fun parseResults(reader: JsonReader) {
            with (reader) {
                beginArray()
                while (hasNext()) {
                    results.add(Movie.Builder(reader).build())
                }
                endArray()
            }
        }

        fun build() = DiscoverResult(page, totalPages, totalResults, results)
    }
}

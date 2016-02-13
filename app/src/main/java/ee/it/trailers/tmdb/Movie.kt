package ee.it.trailers.tmdb

import android.util.JsonReader
import java.io.Serializable

class Movie : Serializable {
    val id: Long
    val imdbId: String
    val title: String
    val posterPath: String
    val originalTitle: String
    val overview: String
    val tagline: String
    val releaseDate: String
    val runtime: Int
    val genreIds: List<Int>
    val genres: List<Genres.Genre>
    val trailers: List<Trailer>
    val cast: List<Cast>
    val crew: List<Crew>

    constructor(builder: Builder) {
        id = builder.id
        imdbId = builder.imdbId
        title = builder.title
        posterPath = builder.posterPath
        originalTitle = builder.originalTitle
        overview = builder.overview
        tagline = builder.tagline
        releaseDate = builder.releaseDate
        runtime = builder.runtime
        genreIds = builder.genreIds ?: listOf()
        genres = builder.genres
        trailers = builder.trailers
        cast = builder.cast
        crew = builder.crew
    }

    class Builder(reader: JsonReader) {
        var id: Long = 0
        var imdbId: String = ""
        var title: String = ""
        var posterPath: String = ""
        var originalTitle: String = ""
        var overview: String = ""
        var tagline: String = ""
        var releaseDate: String = ""
        var runtime: Int = 0
        var genreIds: List<Int>? = null
        val genres: MutableList<Genres.Genre> = mutableListOf()
        val trailers: MutableList<Trailer> = mutableListOf()
        val cast: MutableList<Cast> = mutableListOf()
        val crew: MutableList<Crew> = mutableListOf()

        init {
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "id" -> id = nextLong()
                        "imdb_id" -> imdbId = nextString()
                        "title" -> title = nextString()
                        "poster_path" -> posterPath = nextString()
                        "original_title" -> originalTitle = nextString()
                        "overview" -> overview = nextString()
                        "tagline" -> tagline = nextString()
                        "release_date" -> releaseDate = nextString()
                        "runtime" -> runtime = nextInt()
                        "genre_ids" -> genreIds = parseGenreIds(reader)
                        "genres" -> genres.addAll(Genres.Builder(reader).build().genres)
                        "trailers" -> trailers.addAll(parseTrailers(reader))
                        "credits" -> parseCredits(reader).let {
                            cast.addAll(it.first)
                            crew.addAll(it.second)
                        }
                        else -> skipValue()
                    }
                }
                endObject()
            }
        }

        fun parseGenreIds(reader: JsonReader): List<Int> {
            val genres: MutableList<Int> = mutableListOf()
            with (reader) {
                beginArray()
                while (hasNext()) {
                    genres.add(nextInt())
                }
                endArray()
            }

            return genres
        }

        fun parseTrailers(reader: JsonReader): List<Trailer> {
            val trailers: MutableList<Trailer> = mutableListOf()
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "youtube" -> {
                            beginArray()
                            while (hasNext())
                                trailers.add(Trailer.Builder(reader).build())
                            endArray()
                        }
                        else -> skipValue()
                    }
                }
                endObject()
            }
            return trailers
        }

        fun parseCredits(reader: JsonReader): Pair<List<Cast>, List<Crew>> {
            val cast: MutableList<Cast> = mutableListOf()
            val crew: MutableList<Crew> = mutableListOf()
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "cast" -> {
                            beginArray()
                            while (hasNext())
                                cast.add(Cast.Builder(reader).build())
                            endArray()
                        }
                        "crew" -> {
                            beginArray()
                            while (hasNext())
                                crew.add(Crew.Builder(reader).build())
                            endArray()
                        }
                        else -> skipValue()
                    }
                }
                endObject()
            }
            return Pair(cast, crew)
        }

        fun build() = Movie(this)
    }
}

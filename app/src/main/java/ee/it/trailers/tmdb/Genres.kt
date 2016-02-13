package ee.it.trailers.tmdb

import android.util.JsonReader
import android.util.JsonToken

class Genres(val genres: List<Genre>) {
    data class Genre(val id: Int, val name: String) {
    }

    fun name(id: Int) = genres.find { it.id == id }

    class Builder(reader: JsonReader) {
        val list: MutableList<Genre> = mutableListOf()

        init {
            with (reader) {
                peek().let { token ->
                    if (token == JsonToken.BEGIN_OBJECT) {
                        beginObject()
                        assert("genres".equals(nextName()))
                    }
                }

                beginArray()
                while (hasNext()) {
                    val genre = parseGenre(reader)
                    if (genre != null) {
                        list.add(genre)
                    }
                }
                endArray()
            }
        }

        fun parseGenre(reader: JsonReader): Genre? {
            var id = 0
            var name = ""

            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "id" -> id = nextInt()
                        "name" -> name = nextString()
                        else -> skipValue()
                    }
                }
                endObject()
            }

            if (id != 0 && name.isNotEmpty()) {
                return Genre(id, name)
            } else {
                return null
            }
        }

        fun build(): Genres = Genres(list)
    }
}

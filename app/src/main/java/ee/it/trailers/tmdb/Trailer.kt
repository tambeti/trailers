package ee.it.trailers.tmdb

import android.util.JsonReader

class Trailer(val name: String, val source: String, val type: String, val size: String)  {
    class Builder(reader: JsonReader) {
        var name: String = ""
        var source: String = ""
        var type: String = ""
        var size: String = ""

        init {
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "name" -> name = nextString()
                        "source" -> source = nextString()
                        "type" -> type = nextString()
                        "size" -> type = nextString()
                        else -> skipValue()
                    }
                }
                endObject()
            }
        }

        fun build() = Trailer(name, source, type, size)
    }
}

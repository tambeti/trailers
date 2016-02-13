package ee.it.trailers.tmdb

import android.util.JsonReader

data class Cast(val name: String) {
    class Builder(reader: JsonReader) {
        var name: String = ""

        init {
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "name" -> name = nextString()
                        else -> skipValue()
                    }
                }
                endObject()
            }
        }

        fun build() = Cast(name)
    }
}
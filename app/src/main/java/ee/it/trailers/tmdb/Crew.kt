package ee.it.trailers.tmdb

import android.util.JsonReader

data class Crew(val name: String, val job: String) {
    class Builder(reader: JsonReader) {
        var name: String = ""
        var job: String = ""

        init {
            with (reader) {
                beginObject()
                while (hasNext()) {
                    when (nextName()) {
                        "name" -> name = nextString()
                        "job" -> job = nextString()
                        else -> skipValue()
                    }
                }
                endObject()
            }
        }

        fun build() = Crew(name, job)
    }
}
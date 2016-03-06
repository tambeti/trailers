package ee.it.trailers

import android.net.Uri
import ee.it.trailers.tmdb.Movie
import org.jsoup.Jsoup
import java.io.InputStream

data class Torrent(val name: String, val magnet: String, val size: String,
                   val seeds: Int, val leech: Int) {
}

fun TpbsearchUrl(movie: Movie): String {
    val year = movie.releaseDate.substring(0..3)
    val searchStr = Uri.encode("${movie.title} $year")
    return "https://thepiratebay.se/search/$searchStr/0/99/200";
}

fun Tpbparse(stream: InputStream, url: String): List<Torrent> {
    val doc = Jsoup.parse(stream, null, url)
    val body = doc.body()
            ?.getElementById("main-content")
            ?.getElementsByTag("tbody")
            ?.first()
            ?: throw RuntimeException("Can not find body")

    val sizeRe = Regex("Size (.+),")

    return body.getElementsByClass("vertTh")
            ?.map { it.parent() }
            ?.map { row ->
                val name = row.child(1)
                        ?.getElementsByClass("detLink")
                        ?.map { it.text() }
                        ?.first()
                        ?: ""

                val magnet = row.child(1)
                        ?.child(1)
                        ?.attr("href")
                        ?: ""

                val size = row.child(1)
                        ?.getElementsByClass("detDesc")
                        ?.map {
                            sizeRe.find(it.text())
                                    ?.groups
                                    ?.get(1)
                                    ?.value
                                    ?.replace("iB", "")
                        }
                        ?.first()
                        ?: ""

                val seeds = row.child(2)
                        ?.text()
                        ?.toInt()
                        ?: 0

                val leech = row.child(3)
                        ?.text()
                        ?.toInt()
                        ?: 0

                Torrent(name, magnet, size, seeds, leech)
            }
            ?: emptyList()
}
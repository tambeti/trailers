package ee.it.trailers

import com.squareup.okhttp.Call
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import rx.Single
import java.io.IOException

fun OkHttpClient.get(uri: String): Single<Response> {
    val request = Request.Builder()
            .url(uri)
            .build()
    return newCall(request).observe()
}

fun Call.observe(): Single<Response> = Single.create({ subscriber ->
    try {
        val response = execute()
        if (!subscriber.isUnsubscribed) {
            subscriber.onSuccess(response)
        }
    } catch (e: IOException) {
        if (!subscriber.isUnsubscribed) {
            subscriber.onError(e)
        }
    }
})

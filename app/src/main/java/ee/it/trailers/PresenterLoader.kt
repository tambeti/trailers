package ee.it.trailers

import android.content.Context
import android.support.v4.content.Loader

class PresenterLoader<T : Presenter<*>>(context: Context, private val factory: () -> T) : Loader<T>(context) {
    private var presenter: T? = null

    override fun onStartLoading() {
        if (presenter != null) {
            deliverResult(presenter)
        } else {
            forceLoad()
        }
    }

    override fun onForceLoad() {
        presenter = factory()
        deliverResult(presenter)
    }

    override fun onReset() {
        presenter?.destroy()
        presenter = null
    }
}

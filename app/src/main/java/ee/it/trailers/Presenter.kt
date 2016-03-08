package ee.it.trailers

interface Presenter<V> {
    fun bindView(view: V)
    fun unbindView()
    fun destroy()
}
package ee.it.trailers

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.torrent_item.view.*

class TorrentPickerAdapter(val torrents: List<Torrent>,
                           val listener: TorrentPickerFragment.OnTorrentSelectedListener)
: RecyclerView.Adapter<TorrentPickerAdapter.ViewHolder>() {
    override fun getItemCount() = torrents.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.torrent_item,
                parent, false)
        return ViewHolder(view, listener)

    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, position: Int) {
        viewHolder?.bind(torrents[position])
    }

    class ViewHolder(val view: View, listener: TorrentPickerFragment.OnTorrentSelectedListener)
    : RecyclerView.ViewHolder(view) {
        var torrent: Torrent? = null

        init {
            view.availability
            view.size
            view.title.text = ""

            view.setOnClickListener {
                if (torrent != null) {
                    listener.onTorrentSelected(torrent!!, TorrentPickerFragment.Action.PLAY)
                }
            }

            view.setOnLongClickListener {
                if (torrent != null) {
                    listener.onTorrentSelected(torrent!!, TorrentPickerFragment.Action.COPY)
                }
                true
            }
        }

        fun bind(torrent: Torrent) {
            this.torrent = torrent
            view.availability.text = "${torrent.seeds}/${torrent.leech}"
            view.size.text = torrent.size
            view.title.text = torrent.name
        }
    }
}
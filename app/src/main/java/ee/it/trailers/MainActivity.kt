package ee.it.trailers

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action?.equals(Intent.ACTION_VIEW)?:false) {
            val uri = intent.data
            sendToPulsar(uri.toString())
            finish()
            return
        }

        super.setContentView(R.layout.activity_main)
        setSupportActionBar(my_toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.fragment_container, MoviesFragment())
                    .commit()
        }
    }

    override fun onBackPressed() {
        val fm = fragmentManager
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    fun sendToPulsar(magnet: String) {
        var app = application as MyApplication
        var pulsar = Pulsar(this, app.httpClient)
        pulsar.playUri(magnet)
        Toast.makeText(this, R.string.magnet_sent, Toast.LENGTH_SHORT)
                .show()
    }
}

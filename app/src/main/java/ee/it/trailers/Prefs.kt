package ee.it.trailers

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.f2prateek.rx.preferences.RxSharedPreferences
import rx.Observable

object Prefs {
    private val KEY_KODI_IP = "kodi_ip"
    private val KEY_KODI_PORT = "kodi_port"
    private val KEY_YEAR = "year"
    private val KEY_GENRES = "genres"
    private val DEFAULT_PORT = 8080
    private val TAG = Prefs::class.java.simpleName

    internal fun kodiIP(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_KODI_IP, "")

    internal fun kodiPort(context: Context): Int {
        val str = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_KODI_PORT, Integer.toString(DEFAULT_PORT))
        try {
            return Integer.parseInt(str)
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Can not parse port: " + str)
            return DEFAULT_PORT
        }

    }

    internal fun kodiUrl(context: Context) = kodiIP(context) + ":" + kodiPort(context)

    internal fun year(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_YEAR, 2014)

    fun yearO(context: Context): Observable<Int> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val rxPrefs = RxSharedPreferences.create(prefs)

        return rxPrefs.getInteger(KEY_YEAR, 2014)
                .asObservable()
    }

    internal fun year(context: Context, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_YEAR, value)
                .apply()
    }

    internal fun genres(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(KEY_GENRES, emptySet<String>())
                .map { it.toInt() }

    fun generesO(context: Context): Observable<Set<Int>> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val rxPrefs = RxSharedPreferences.create(prefs)

        return rxPrefs.getStringSet(KEY_GENRES, emptySet())
                .asObservable()
                .map { set ->
                    set.map {
                        it.toInt()
                    }.toSet()
                }
    }

    internal fun genres(context: Context, value: List<Int>) {
        var set = value.map { it.toString() }
                .toSet()

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(KEY_GENRES, set)
                .apply()
    }
}

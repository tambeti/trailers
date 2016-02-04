package ee.it.trailers;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Prefs {
    private static final String KEY_KODI_IP = "kodi_ip";
    private static final String KEY_KODI_PORT = "kodi_port";
    private static final String KEY_YEAR = "year";
    private static final String KEY_GENRES = "genres";
    private static final int DEFAULT_PORT = 8080;
    private static final String TAG = Prefs.class.getSimpleName();

    static String kodiIP(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_KODI_IP, "");
    }

    static int kodiPort(Context context) {
        final String str = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_KODI_PORT, Integer.toString(DEFAULT_PORT));
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Can not parse port: " + str);
            return DEFAULT_PORT;
        }
    }

    static String kodiUrl(Context context) {
        return kodiIP(context) + ":" + kodiPort(context);
    }

    static int year(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_YEAR, 2014);
    }

    static void year(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(KEY_YEAR, value)
                .apply();
    }

    static List<Integer> genres(Context context) {
        final Set<String> set = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(KEY_GENRES, Collections.<String>emptySet());

        final List<Integer> list = new ArrayList<>(set.size());
        for (String s : set) {
            list.add(Integer.parseInt(s));
        }

        return list;
    }

    static void genres(Context context, List<Integer> value) {
        final Set<String> set = new HashSet<>();
        for (Integer i : value) {
            set.add(i.toString());
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(KEY_GENRES, set)
                .apply();
    }
}

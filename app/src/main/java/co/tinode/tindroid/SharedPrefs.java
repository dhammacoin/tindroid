package co.tinode.tindroid;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefs {

    private static final String PREFS_NAME = "tinode_prefs";
    private static final String KEY_TOKEN = "auth_token";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ====== TOKEN ======

    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    public static void deleteToken(Context context) {
        getPrefs(context).edit().remove(KEY_TOKEN).apply();
    }
}

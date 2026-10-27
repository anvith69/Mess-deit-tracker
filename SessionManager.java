package com.example.miniproject.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers which user is logged in (SharedPreferences = small key/value storage). */
public class SessionManager {

    private static final String PREFS = "mess_diet_session";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveUser(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    /** @return the logged-in user's id, or -1 when nobody is logged in. */
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

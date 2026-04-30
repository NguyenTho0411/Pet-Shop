package com.example.petshop.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private static final String PREF_NAME    = "petshop_prefs";
    private static final String KEY_ROLE     = "user_role";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_NAME     = "user_name";
    private static final String KEY_EMAIL    = "user_email";
    private static final String KEY_AVATAR   = "user_avatar";

    private final SharedPreferences prefs;

    private static SessionManager instance;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveSession(String userId, String name, String email, String role, String avatarUrl) {
        prefs.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE, role)
                .putString(KEY_AVATAR, avatarUrl)
                .apply();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public String getUserId()   { return prefs.getString(KEY_USER_ID, null); }
    public String getUserName() { return prefs.getString(KEY_NAME, ""); }
    public String getUserEmail(){ return prefs.getString(KEY_EMAIL, ""); }
    public String getRole()     { return prefs.getString(KEY_ROLE, ROLE_CUSTOMER); }
    public String getAvatarUrl(){ return prefs.getString(KEY_AVATAR, null); }
    public boolean isLoggedIn() { return getUserId() != null; }
    public boolean isAdmin()    { return ROLE_ADMIN.equals(getRole()); }
}

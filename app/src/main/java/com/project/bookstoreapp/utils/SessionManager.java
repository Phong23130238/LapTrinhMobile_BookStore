package com.project.bookstoreapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.project.bookstoreapp.model.User;

public class SessionManager {
    private static final String PREF_NAME = "BookStoreSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_DATA = "userData";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    public void saveUser(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        if (user != null) {
            editor.putString(KEY_USER_ROLE, user.getRole());
            String userJson = gson.toJson(user);
            editor.putString(KEY_USER_DATA, userJson);
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "");
    }

    public User getUser() {
        String userJson = sharedPreferences.getString(KEY_USER_DATA, null);
        if (userJson != null) {
            return gson.fromJson(userJson, User.class);
        }
        return null;
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}

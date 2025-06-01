package com.mehmetalper04.sinavgram.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.mehmetalper04.sinavgram.models.User; // User modelini import et

public class TokenManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private static final String PREF_NAME = Constants.PREF_NAME; // Constants sınıfından alalım
    private static final String KEY_AUTH_TOKEN = Constants.KEY_AUTH_TOKEN;
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_IS_PREMIUM = "user_is_premium";
    private static final String KEY_HAS_ACTIVE_PREMIUM = "user_has_active_premium";
    private static final String KEY_PREMIUM_EXP_DATE = "user_premium_exp_date";


    private static TokenManager INSTANCE = null;

    private TokenManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new TokenManager(context.getApplicationContext());
        }
        return INSTANCE;
    }

    public void saveUserLoginInfo(User user, String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        if (user != null) {
            editor.putInt(KEY_USER_ID, user.getId());
            editor.putString(KEY_USERNAME, user.getUsername());
            editor.putString(KEY_EMAIL, user.getEmail());
            editor.putBoolean(KEY_IS_PREMIUM, user.isPremium()); // Backend'den gelen is_premium
            editor.putBoolean(KEY_HAS_ACTIVE_PREMIUM, user.hasActivePremium()); // Backend'den gelen has_active_premium
            editor.putString(KEY_PREMIUM_EXP_DATE, user.getPremiumExpirationDate());
        }
        editor.apply();
    }

    public String getToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public boolean hasToken() {
        return getToken() != null;
    }

    public boolean hasActivePremium() {
        // Aktif premium, hem is_premium true hem de geçerli bir son kullanım tarihi olmalı.
        // Backend zaten has_active_premium gönderiyor, onu direkt kullanalım.
        return prefs.getBoolean(KEY_HAS_ACTIVE_PREMIUM, false);
    }

    // Diğer kullanıcı bilgilerini almak için getter'lar eklenebilir (username, email vb.)
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }


    public void clearToken() { // Token ve kullanıcı bilgilerini temizle
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_IS_PREMIUM);
        editor.remove(KEY_HAS_ACTIVE_PREMIUM);
        editor.remove(KEY_PREMIUM_EXP_DATE);
        editor.apply();
    }
}
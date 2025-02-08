package com.example.qr_code_project.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.auth0.android.jwt.JWT;
import com.example.qr_code_project.activity.login.LoginActivity;

import java.util.Date;

public class TokenManager {

    private static final String PREF_NAME = "AccountToken";
    private static final String TOKEN_KEY = "token";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    public TokenManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return sharedPreferences.getString(TOKEN_KEY, null);
    }

    public boolean isTokenExpired() {
        String token = getToken();
        if (token == null) {
            return true;
        }
        try {
            JWT jwt = new JWT(token);
            Date expiresAt = jwt.getExpiresAt();
            return expiresAt != null && expiresAt.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public void clearTokenAndLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TOKEN_KEY);
        editor.apply();

        Toast.makeText(context, "Your session has expired. Please log in again."
                , Toast.LENGTH_LONG).show();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}

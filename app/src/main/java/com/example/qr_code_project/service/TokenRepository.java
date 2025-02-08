package com.example.qr_code_project.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.network.ApiConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TokenRepository {

    public static void sendTokenToServer(Context context, String token) {
        TokenManager tokenManager = new TokenManager(context);
        RequestQueue queue = Volley.newRequestQueue(context);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences("AccountToken", Context.MODE_PRIVATE);
        String url = ApiConstants.TOKEN_ADD;

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");

                        if (isSuccess) {
                            Log.d("FCM", "Token sent successfully");
                        } else {
                            Log.d("FCM", "Token sent failed");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("FCM", "Failed to send token: " + error.toString())
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("token", token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("token", "Request Body: " + params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (!tokenManager.isTokenExpired()) {
                    headers.put("Authorization", "Bearer " + token);
                }else {
                    tokenManager.clearTokenAndLogout();
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }
}

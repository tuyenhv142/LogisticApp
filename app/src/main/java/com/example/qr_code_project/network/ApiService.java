package com.example.qr_code_project.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiService {
    private final Context context;
//    private TokenManager tokenManager
    private final RequestQueue requestQueue;
    private final SharedPreferences sharedPreferences;
    private static final String TAG = "ApiService";

    public interface ApiResponseListener {
        void onSuccess(String response);
        void onError(String error);
    }

    public ApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.sharedPreferences = context.getSharedPreferences("AccountToken", Context.MODE_PRIVATE);
    }

    public void submitInbound(String code, int quantity, ApiResponseListener listener) {
        String url = ApiConstants.INBOUND_SUBMIT;
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");
                        String message = jsonObject.optString("error", "Unknown error");

                        if (isSuccess) {
                            listener.onSuccess("Submit successful!");
                        } else {
                            listener.onError(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onError("Failed to parse response!");
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e(TAG, "Status Code: " + statusCode + ", Response: " + responseBody);
                        listener.onError("Error " + statusCode + ": " + responseBody);
                    } else {
                        Log.e(TAG, "Error: " + error.toString());
                        listener.onError("Unknown error occurred.");
                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("code", code);
                    params.put("actualQuantity", quantity);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Request Body: " + params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public void submitPackage(String code, int deliveryId, ApiResponseListener listener) {
        String url = ApiConstants.PACKAGE_SUBMIT;
        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");
                        String message = jsonObject.optString("error", "Unknown error");

                        if (isSuccess) {
                            listener.onSuccess("Submit successful!");
                        } else {
                            listener.onError(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onError("Failed to parse response!");
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e(TAG, "Status Code: " + statusCode + ", Response: " + responseBody);
                        listener.onError("Error " + statusCode + ": " + responseBody);
                    } else {
                        Log.e(TAG, "Error: " + error.toString());
                        listener.onError("Unknown error occurred.");
                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("code", code);
                    params.put("id", deliveryId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Request Body: " + params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    public void submitSwap(int statusId, ApiResponseListener listener) {
        String url = ApiConstants.SWAP_LOCATION_SUBMIT;

        StringRequest request = new StringRequest(Request.Method.PUT, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean isSuccess = jsonObject.getBoolean("success");
                        String message = jsonObject.optString("error", "Unknown error");

                        if (isSuccess) {
                            listener.onSuccess("Submit successful!");
                        } else {
                            listener.onError(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        listener.onError("Failed to parse response!");
                    }
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseBody = new String(error.networkResponse.data);
                        Log.e(TAG, "Status Code: " + statusCode + ", Response: " + responseBody);
                        listener.onError("Error " + statusCode + ": " + responseBody);
                    } else {
                        Log.e(TAG, "Error: " + error.toString());
                        listener.onError("Unknown error occurred.");
                    }
                }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                JSONObject params = new JSONObject();
                try {
                    params.put("id_statuswarehourse", statusId);
                    params.put("title", "done");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Request Body: " + params.toString());
                return params.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (token != null) {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }


}

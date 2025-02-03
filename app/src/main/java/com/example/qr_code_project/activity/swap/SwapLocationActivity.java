package com.example.qr_code_project.activity.swap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.activity.inbound.ConfirmInboundActivity;
import com.example.qr_code_project.activity.inbound.InboundActivity;
import com.example.qr_code_project.activity.login.LoginActivity;
import com.example.qr_code_project.activity.outbound.ExportDetailActivity;
import com.example.qr_code_project.activity.outbound.OutboundActivity;
import com.example.qr_code_project.adapter.ExportAdapter;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.adapter.SwapLocationAdapter;
import com.example.qr_code_project.modal.ExportModal;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.modal.SwapModal;
import com.example.qr_code_project.network.ApiConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SwapLocationActivity extends AppCompatActivity implements SwapLocationAdapter.OnSwapClickListener {

    private RecyclerView swapLocationsRv;
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private ArrayList<SwapModal> swapArrayList;
    private SwapLocationAdapter swapLocationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_swap_location);

        util();

        loadSwapPlan();
    }

    private void util(){
        swapLocationsRv = findViewById(R.id.swapLocationsRv);

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        swapLocationsRv.setLayoutManager(new LinearLayoutManager(this));
        swapArrayList = new ArrayList<>();
    }

    private void loadSwapPlan(){
        String url = ApiConstants.SWAP_LOCATION;
        StringRequest request = new StringRequest(
                Request.Method.GET,url,
                this::parseResponse,
                this::handleError
        ) {
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

    @SuppressLint("NotifyDataSetChanged")
    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    populateContent(content);
                }
            } else {
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("responseValue", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        JSONArray swapLocations = content.optJSONArray("data");
        swapArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(swapLocations).length(); i++) {
            JSONObject object = swapLocations.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            String locationOldCode = object.optString("localtionOldCode","N/A");
            String locationNewCode = object.optString("localtionNewCode", "N/A");
            String warehouseOld = object.optString("warehouseOld", "N/A");
            String areaOld = object.optString("areaOld", "N/A");
            String floorOld = object.optString("floorOld","N/A");
            String warehouse = object.optString("warehouse", "N/A");
            String area = object.optString("area", "N/A");
            String floor = object.optString("floor", "N/A");

            swapArrayList.add(new SwapModal(floor, area, warehouse
                    , floorOld, areaOld, warehouseOld
                    ,locationNewCode,locationOldCode,title,id));
        }

        if (swapLocationAdapter == null) {
            swapLocationAdapter = new SwapLocationAdapter(this
                    , swapArrayList,this );

            swapLocationsRv.setAdapter(swapLocationAdapter);
        } else {
            swapLocationAdapter.notifyDataSetChanged();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(Exception error) {
        String errorMsg = "An error occurred. Please try again.";
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = "Request timed out. Please check your connection.";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = "No internet connection!";
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSwapItemClick(int swapId) {
        showConfirmationDialog(swapId);
    }

    private void showConfirmationDialog(int swapId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Task")
                .setMessage("Do you want to accept this task ?")
                .setPositiveButton("Yes", (dialog, which) -> sendConfirmationRequest(swapId))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void sendConfirmationRequest(int swapId) {
        String url = ApiConstants.CONFIRM_SWAP_LOCATION;
        Log.d("swapID",""+swapId);

        StringRequest request = new StringRequest(Request.Method.PUT, url,
            response -> {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
//                    if (jsonObject.getBoolean("content")) {
                    if (jsonObject.getBoolean("success")) {
                        showError(jsonObject.optString("error", "Unknown error"));
                        Toast.makeText(this, "Confirmed successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, DetailSwapLocationActivity.class);
                        intent.putExtra("swapId", swapId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            },
            error -> Toast.makeText(this, "Error confirming task", Toast.LENGTH_SHORT).show()
        )
        {
            @Override
            public byte[] getBody() {
                JSONObject params = new JSONObject();
                try {
                    JSONArray idArray = new JSONArray();
                    idArray.put(swapId);
                    params.put("id", idArray);
                    params.put("isConfirmation", true);
                } catch (JSONException e) {
                    Log.e("JSONError", "Error while creating JSON body", e);
                }
                Log.d("Request Body", params.toString());
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

        Volley.newRequestQueue(this).add(request);
    }
}
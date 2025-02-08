package com.example.qr_code_project.activity.swap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.activity.packaged.PackageActivity;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.ApiService;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfirmSwapLocationActivity extends AppCompatActivity {

    private TextView nameLocationOld1Tv,nameLocationOld2Tv,
            nameLocationNew1Tv,nameLocationNew2Tv;
    private TextView locationNewBarcodeStatus1,locationNewBarcodeStatus2;
    private EditText nameProductLocation1,quantityProductLocation1
            ,codeProductLocation1,codeLocation2;
    private EditText nameProductLocation2,quantityProductLocation2
            ,codeProductLocation2,codeLocation1;
    private Button confirmSwapBtn;
    private String product1Location,product2Location;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private QRcodeManager qrCodeManager;
    private String scannedLocation1 = "";
    private String scannedLocation2= "";
    private ApiService apiService;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;

    private int statusId;
    private int swapId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_confirm_swap_location);


        util();

        setupQRManager();

        fetchProductLocation(product1Location,true);

        fetchProductLocation(product2Location,false);

        fetchPlanStatus(swapId);

        confirmSwapBtn.setOnClickListener(v -> onConfirmSwap(swapId));
    }

    private void onConfirmSwap(int swapId) {
        if (scannedLocation1.isEmpty() || scannedLocation2.isEmpty()) {
            Toast.makeText(this, "Please scan both location barcodes before confirming!"
                    , Toast.LENGTH_SHORT).show();
            return;
        }
        loadingDialog.show();
        apiService.submitSwap(swapId, new ApiService.ApiResponseListener() {
            @Override
            public void onSuccess(String response) {
                loadingDialog.dismiss();
                Toast.makeText(ConfirmSwapLocationActivity.this, response, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ConfirmSwapLocationActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                loadingDialog.dismiss();
                Toast.makeText(ConfirmSwapLocationActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
    }

    private void handleScannedData(String qrCodeText) {

        if (scannedLocation2.isEmpty()) {
            scannedLocation2 = qrCodeText;
            if (scannedLocation2.equals(codeLocation2.getText().toString().trim())) {
                updateLocation1ScanStatus(true, "Location barcode is correct.");
            } else {
                updateLocation1ScanStatus(false, "Invalid location barcode! Please scan again.");
                scannedLocation2 = "";
            }
        }else if (scannedLocation1.isEmpty()) {
            scannedLocation1 = qrCodeText;
            if (scannedLocation1.equals(codeLocation1.getText().toString().trim())) {
                updateLocation2ScanStatus(true, "Location barcode is correct.");
                confirmSwapBtn.setEnabled(true);
                if (qrCodeManager != null) {
                    qrCodeManager.unregister();
                    qrCodeManager = null;
                }
            } else {
                updateLocation2ScanStatus(false, "Invalid Location barcode! Please scan again.");
                scannedLocation1 = "";
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }

    private void updateLocation1ScanStatus(boolean isValid, String message) {
        locationNewBarcodeStatus1.setVisibility(View.VISIBLE);
        locationNewBarcodeStatus1.setText(message);
        locationNewBarcodeStatus1.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateLocation2ScanStatus(boolean isValid, String message) {
        locationNewBarcodeStatus2.setVisibility(View.VISIBLE);
        locationNewBarcodeStatus2.setText(message);
        locationNewBarcodeStatus2.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    @SuppressLint("SetTextI18n")
    private void util(){
        nameLocationOld1Tv = findViewById(R.id.nameLocationOld1Tv);
        nameLocationOld2Tv = findViewById(R.id.nameLocationOld2Tv);
        nameLocationNew1Tv = findViewById(R.id.nameLocationNew1Tv);
        nameLocationNew2Tv = findViewById(R.id.nameLocationNew2Tv);
        locationNewBarcodeStatus1 = findViewById(R.id.locationNewBarcodeStatus1);
        locationNewBarcodeStatus2 = findViewById(R.id.locationNewBarcodeStatus2);
        nameProductLocation1 = findViewById(R.id.nameProductLocation1);
        quantityProductLocation1 = findViewById(R.id.quantityProductLocation1);
        codeProductLocation1 = findViewById(R.id.codeProductLocation1);
        codeLocation2 = findViewById(R.id.codeLocation2);
        nameProductLocation2 = findViewById(R.id.nameProductLocation2);
        quantityProductLocation2 = findViewById(R.id.quantityProductLocation2);
        codeProductLocation2 = findViewById(R.id.codeProductLocation2);
        codeLocation1 = findViewById(R.id.codeLocation1);
        confirmSwapBtn = findViewById(R.id.confirmSwapBtn);

        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);

        product1Location = sharedPreferences.getString("product1_location", "N/A");
        product2Location = sharedPreferences.getString("product2_location", "N/A");
        swapId = sharedPreferences.getInt("swap_location_id", 0);
        nameLocationOld1Tv.setText("Location code: "+product1Location);
        nameLocationOld2Tv.setText("Location code: "+product2Location);
        nameLocationNew1Tv.setText("Location code new: ");
        nameLocationNew2Tv.setText("Location code new: ");
        codeLocation2.setText(product2Location);
        codeLocation1.setText(product1Location);
        requestQueue = Volley.newRequestQueue(this);
        confirmSwapBtn.setEnabled(false);
        apiService = new ApiService(this);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
    }

    private void fetchProductLocation(String code, boolean isOldLocation) {
        String url = ApiConstants.getFindCodeLocationProductUrl(code);
        Log.d("url_getFindCodeLocationProductUrl",url);
        loadingDialog.show();
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                response -> parseResponseProductLocation(response, isOldLocation),
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (!tokenManager.isTokenExpired()) {
                    headers.put("Authorization", "Bearer " + token);
                }else{
                    tokenManager.clearTokenAndLogout();
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // timeout (30 second)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    private void parseResponseProductLocation(String response, boolean isOldLocation) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONArray content = jsonObject.optJSONArray("content");
                if (content != null) {
                    for (int i = 0; i < content.length(); i++) {
                        JSONObject product = content.getJSONObject(i);

                        if (isOldLocation) {
                            nameProductLocation1.setText(product.optString("title", "N/A"));
                            quantityProductLocation1.setText(String.valueOf(product.optInt("quantity", 0)));
                            codeProductLocation1.setText(product.optString("code", "N/A"));
                        } else {
                            nameProductLocation2.setText(product.optString("title", "N/A"));
                            quantityProductLocation2.setText(String.valueOf(product.optInt("quantity", 0)));
                            codeProductLocation2.setText(product.optString("code", "N/A"));
                        }
                    }
                }
            } else {
                Log.d("error", "Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to parse response!", Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void handleError(Exception error) {
        String errorMsg = "An error occurred. Please try again.";
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = "Request timed out. Please check your connection.";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = "No internet connection!";
        }
        loadingDialog.dismiss();
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
    }

    private void fetchPlanStatus(int code) {
        String url = ApiConstants.getFindByPlanUrl(code);
        StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
                this::handleError
        ) {
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
        requestQueue.add(request);
    }

    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    statusId = content.optInt("id_status",0);
                }
            } else {
                Log.d("error", "Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to parse response!", Toast.LENGTH_SHORT).show();
        }
    }

}
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.inbound.ConfirmInboundActivity;
import com.example.qr_code_project.activity.inbound.InboundActivity;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DetailSwapLocationActivity extends AppCompatActivity {

    private EditText warehouseOldEd,floorOldEd,areaOldEt,locationOld1
            ,productName1,productQuantity1,productCode1;
    private EditText warehouseNew2,floorNew2,areaNew2,locationNewCode2
            ,productName2,productQuantity2,productCode2;
    private TextView locationBarcodeStatus1Text, productBarcodeStatus1Text;
    private TextView locationBarcodeStatus2Text, productBarcodeStatus2Text;
    private Button continueConfirmProductBtn;
    private String scannedLocation1 = "";
    private String scannedProductLocation1 = "";
    private String scannedLocation2= "";
    private String scannedProductLocation2 = "";
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private QRcodeManager qrCodeManager;
    private boolean isConfirmed = false;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_swap_location);

        util();

        Integer swapId = getSwapIdFromIntent();
        if (swapId == null) return;

        loadSwapLocation(swapId);

        setupQRManager();

        continueConfirmProductBtn.setOnClickListener(v -> onConfirmSwap(swapId));

    }

    private @Nullable Integer getSwapIdFromIntent() {
        int swapId = getIntent().getIntExtra("swapId", 0);

        if (swapId == 0) {
            Toast.makeText(this, "Error swapId is empty!", Toast.LENGTH_SHORT).show();
            return null;
        }
        return swapId;
    }

    private void onConfirmSwap(int swapId) {
        if (scannedProductLocation1.isEmpty() || scannedProductLocation2.isEmpty()) {
            Toast.makeText(this, "Please scan both product barcodes before confirming!"
                    , Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save swapped product data in SharedPreferences
        editor.putString("product1_location", locationNewCode2.getText().toString().trim());
        editor.putString("product2_location", locationOld1.getText().toString().trim());
        editor.putInt("swap_location_id", swapId);

        editor.apply(); // Commit changes

        Toast.makeText(this, "Product swap confirmed!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(DetailSwapLocationActivity.this, ConfirmSwapLocationActivity.class);
        startActivity(intent);
        finish();
    }

    //Get data Inbound from Api
    private void loadSwapLocation(int swapId) {
        String url = ApiConstants.getFindOneCodeSwapLocationUrl(swapId);
        StringRequest findInbound = new StringRequest(
                Request.Method.GET, url,
                this::parseResponseLocation,
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

        requestQueue.add(findInbound);
    }

    //Data from Api
    private void parseResponseLocation(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    locationOld1.setText(content.optString("localtionOldCode", "N/A"));
                    locationNewCode2.setText(content.optString("localtionNewCode", "N/A"));
                    warehouseOldEd.setText(content.optString("warehouseOld", "N/A"));
                    areaOldEt.setText(content.optString("areaOld", "N/A"));
                    floorOldEd.setText(content.optString("floorOld", "N/A"));
                    warehouseNew2.setText(content.optString("warehouse", "N/A"));
                    areaNew2.setText(content.optString("area", "N/A"));
                    floorNew2.setText(content.optString("floor", "N/A"));
                }
            } else {
                showError(jsonObject.optString("error", "Unknown error"));
            }
        } catch (JSONException e) {
            Log.e("SwapLocation", "Failed to parse JSON response", e);
            showError("Failed to parse response!");
        }
    }

    private void util(){
        warehouseOldEd = findViewById(R.id.warehouseOldEd);
        floorOldEd = findViewById(R.id.floorOldEd);
        areaOldEt = findViewById(R.id.areaOldEt);
        locationOld1 = findViewById(R.id.locationOld1);
        continueConfirmProductBtn = findViewById(R.id.continueConfirmProductBtn);
        productName1 = findViewById(R.id.productName1);
        productQuantity1 = findViewById(R.id.productQuantity1);
        productCode1 = findViewById(R.id.productCode1);
        warehouseNew2 = findViewById(R.id.warehouseNew2);
        floorNew2 = findViewById(R.id.floorNew2);
        areaNew2 = findViewById(R.id.areaNew2);
        locationNewCode2 = findViewById(R.id.locationNewCode2);
        productName2 = findViewById(R.id.productName2);
        productQuantity2 = findViewById(R.id.productQuantity2);
        productCode2 = findViewById(R.id.productCode2);
        locationBarcodeStatus1Text = findViewById(R.id.locationBarcodeStatus1Text);
        productBarcodeStatus1Text = findViewById(R.id.productBarcodeStatus1Text);
        locationBarcodeStatus2Text = findViewById(R.id.locationBarcodeStatus2Text);
        productBarcodeStatus2Text = findViewById(R.id.productBarcodeStatus2Text);

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
        continueConfirmProductBtn.setEnabled(false);
    }

    //Show error
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }

    private void handleScannedData(String qrCodeText) {
        if (isConfirmed) return;

        if (scannedLocation1.isEmpty()) {
            scannedLocation1 = qrCodeText;
            if (scannedLocation1.equals(locationOld1.getText().toString().trim())) {
                updateLocation1ScanStatus(true, "Location barcode is correct.");
                fetchProductLocation(scannedLocation1,true);
            } else {
                updateLocation1ScanStatus(false, "Invalid location barcode! Please scan again.");
                scannedLocation1 = "";
            }
        } else if (scannedProductLocation1.isEmpty()) {
            scannedProductLocation1 = qrCodeText;
            if (scannedProductLocation1.equals(productCode1.getText().toString().trim())) {
                updateProductLocation1ScanStatus(true, "Product barcode is correct.");
            } else {
                updateProductLocation1ScanStatus(false, "Invalid Product barcode! Please scan again.");
                scannedProductLocation1 = "";
            }
        }else if (scannedLocation2.isEmpty()) {
            scannedLocation2 = qrCodeText;
            if (scannedLocation2.equals(locationNewCode2.getText().toString().trim())) {
                updateLocation2ScanStatus(true, "Location barcode is correct.");
                fetchProductLocation(scannedLocation2,false);
            } else {
                updateLocation2ScanStatus(false, "Invalid Location barcode! Please scan again.");
                scannedLocation2 = "";
            }
        }else if (scannedProductLocation2.isEmpty()) {
            scannedProductLocation2 = qrCodeText;
            if (scannedProductLocation2.equals(productCode2.getText().toString().trim())) {
                updateProductLocation2ScanStatus(true, "Product barcode is correct.");
                continueConfirmProductBtn.setEnabled(true);
                if (qrCodeManager != null) {
                    qrCodeManager.unregister();
                    qrCodeManager = null;
                }
            } else {
                updateProductLocation2ScanStatus(false, "Invalid Product barcode! Please scan again.");
                scannedProductLocation2 = "";
            }
        }
    }

    private void updateLocation1ScanStatus(boolean isValid, String message) {
        locationBarcodeStatus1Text.setVisibility(View.VISIBLE);
        locationBarcodeStatus1Text.setText(message);
        locationBarcodeStatus1Text.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateProductLocation1ScanStatus(boolean isValid, String message) {
        productBarcodeStatus1Text.setVisibility(View.VISIBLE);
        productBarcodeStatus1Text.setText(message);
        productBarcodeStatus1Text.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateLocation2ScanStatus(boolean isValid, String message) {
        locationBarcodeStatus2Text.setVisibility(View.VISIBLE);
        locationBarcodeStatus2Text.setText(message);
        locationBarcodeStatus2Text.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateProductLocation2ScanStatus(boolean isValid, String message) {
        productBarcodeStatus2Text.setVisibility(View.VISIBLE);
        productBarcodeStatus2Text.setText(message);
        productBarcodeStatus2Text.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void fetchProductLocation(String code, boolean isOldLocation) {
        String url = ApiConstants.getFindCodeLocationProductUrl(code);
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
                }else {
                    tokenManager.clearTokenAndLogout();
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
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
                            productName1.setText(product.optString("title", "N/A"));
                            productQuantity1.setText(String.valueOf(product.optInt("quantity", 0)));
                            productCode1.setText(product.optString("code", "N/A"));
                        } else {
                            productName2.setText(product.optString("title", "N/A"));
                            productQuantity2.setText(String.valueOf(product.optInt("quantity", 0)));
                            productCode2.setText(product.optString("code", "N/A"));
                        }
                    }
                }
            } else {
                Toast.makeText(this,"Data null",Toast.LENGTH_SHORT).show();
                if (isOldLocation){
                    updateProductLocation1ScanStatus(true, "Can skip this code");
                }else {
                    updateProductLocation2ScanStatus(true, "Can skip this code");
                }

                Log.d("error", "Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Failed to parse response!", Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    //Show error process Api
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

}
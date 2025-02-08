package com.example.qr_code_project.activity.outbound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConfirmOutboundActivity extends AppCompatActivity {

    private EditText barcodeOutboundEt, nameOutboundEt, quantityOutboundEt, realQuantityOutboundEt
            , warehouseCodeOutboundEt;
    private Button confirmOutboundBtn;
//    private ImageView productBarcodeStatusOutboundIcon, warehouseBarcodeStatusOutboundIcon;
    private TextView productBarcodeStatusOutboundText, warehouseBarcodeStatusOutboundText;

    private QRcodeManager qrCodeManager;
    private String scannedProductBarcode = "";
    private String scannedWarehouseBarcode = "";
    private SharedPreferences sharedPreferences;
    private RequestQueue requestQueue;
    private String code;
    private int areaId;
    private int location;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;

    private boolean isConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_outbound);

        initViews();

        setupQRManager();

        getDateFromIntent();
    }

    private void getDateFromIntent() {
        ProductModal productModal = (ProductModal) getIntent().getSerializableExtra("product");
        if (productModal == null) {
            Toast.makeText(this, "Product data is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        barcodeOutboundEt.setText(productModal.getCode());
        nameOutboundEt.setText(productModal.getTitle());
        quantityOutboundEt.setText(String.valueOf(productModal.getQuantity()));

        Map<Integer, Object>  productMap =
                (Map<Integer, Object> ) getIntent().getSerializableExtra("productMap");
        if (productMap == null) {
            productMap = new HashMap<>();
        }

        Map<Integer, Object>  finalProductMap = productMap;

        confirmOutboundBtn.setOnClickListener(v -> handleConfirmation(productModal, finalProductMap));
    }

    // Initialize views and shared preferences
    private void initViews() {
        barcodeOutboundEt = findViewById(R.id.barcodeOutboundEt);
        nameOutboundEt = findViewById(R.id.nameOutboundEt);
        quantityOutboundEt = findViewById(R.id.quantityOutboundEt);
        realQuantityOutboundEt = findViewById(R.id.realQuantityOutboundEt);
        warehouseCodeOutboundEt = findViewById(R.id.warehouseCodeOutboundEt);
        confirmOutboundBtn = findViewById(R.id.confirmOutboundBtn);
        confirmOutboundBtn.setVisibility(View.GONE);
//        productBarcodeStatusOutboundIcon = findViewById(R.id.productBarcodeStatusOutboundIcon);
//        warehouseBarcodeStatusOutboundIcon = findViewById(R.id.warehouseBarcodeStatusOutboundIcon);
        productBarcodeStatusOutboundText = findViewById(R.id.productBarcodeStatusOutboundText);
        warehouseBarcodeStatusOutboundText = findViewById(R.id.warehouseBarcodeStatusOutboundText);

        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        loadingDialog = new LoadingDialog(this);
        tokenManager = new TokenManager(this);
    }

    private void setupQRManager() {
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);
    }

    private void handleConfirmation(ProductModal productModal, Map<Integer, Object> productMap) {
        String orderQuantityStr = quantityOutboundEt.getText().toString().trim();
        String realQuantityStr = realQuantityOutboundEt.getText().toString().trim();

        if (realQuantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter actual quantity!", Toast.LENGTH_SHORT).show();
            return;
        }

        int orderQuantity = Integer.parseInt(orderQuantityStr);
        int actualQuantity = Integer.parseInt(realQuantityStr);

        if (actualQuantity != orderQuantity) {
            showConfirmationDialog(orderQuantity, actualQuantity, productModal, productMap);
        } else {
            confirmProduct(productModal, productMap, actualQuantity);
        }
    }

    private void handleScannedData(String qrCodeText) {
        if (isConfirmed) return;

        if (scannedProductBarcode.isEmpty()) {
            scannedProductBarcode = qrCodeText;
            if (scannedProductBarcode.equals(barcodeOutboundEt.getText().toString().trim())) {
                updateProductScanStatus(true, "Product barcode is valid.");
                fetchWarehouseLocation(scannedProductBarcode);
            } else {
                updateProductScanStatus(false, "Invalid product barcode! Please scan again.");
                scannedProductBarcode = "";
            }
        } else if (scannedWarehouseBarcode.isEmpty()) {
            scannedWarehouseBarcode = qrCodeText;
            if (scannedWarehouseBarcode.equals(warehouseCodeOutboundEt.getText().toString().trim())) {
                updateWarehouseScanStatus(true, "Warehouse barcode is valid.");
                confirmOutboundBtn.setVisibility(View.VISIBLE);
                if (qrCodeManager != null) {
                    qrCodeManager.unregister();
                    qrCodeManager = null;
                }
            } else {
                updateWarehouseScanStatus(false, "Invalid warehouse barcode! Please scan again.");
                scannedWarehouseBarcode = "";
            }
        }
    }

    private void fetchWarehouseLocation(String code) {
        String url = ApiConstants.getFindOneCodeProductUrl(code);
        loadingDialog.show();
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
                    populateContent(content);
                }
            } else {
                Log.d("error","Unknown error");
            }
        } catch (JSONException e) {
            Toast.makeText(this,"Failed to parse response!",Toast.LENGTH_SHORT).show();
        }finally {
            loadingDialog.dismiss();
        }
    }

    private void populateContent(JSONObject content) throws JSONException {
        int quantity = content.optInt("quantity", 0);
        JSONArray warehouseArray = content.optJSONArray("listAreaOfproducts");
        if (warehouseArray == null || warehouseArray.length() == 0) {
            Toast.makeText(this,"No warehouses found for this product.",Toast.LENGTH_SHORT).show();
            return;
        }

        int requiredQuantity = Integer.parseInt(quantityOutboundEt.getText().toString().trim());
        for (int i = 0; i < warehouseArray.length(); i++) {
            JSONObject warehouse = warehouseArray.getJSONObject(i);

            code = warehouse.optString("code", "N/A");
            areaId = warehouse.optInt("idArea", 0);
            location = warehouse.optInt("location", 0);
            if (quantity >= requiredQuantity) {
                warehouseCodeOutboundEt.setText(code);
                return;
            }
        }

        Toast.makeText(this,"No warehouse has enough products for the order.",Toast.LENGTH_SHORT).show();
    }

    private void confirmProduct(ProductModal productModal, Map<Integer, Object>  productMap, int actualQuantity) {
        isConfirmed = true;
        confirmOutboundBtn.setEnabled(false);

        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("actualQuantity", actualQuantity);
        productInfo.put("code", code);
        productInfo.put("areaId", areaId);
        productInfo.put("location", location);

        productMap.put(productModal.getId(), productInfo);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("productMap", new HashMap<>(productMap));
        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Product confirmed successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showConfirmationDialog(int orderQuantity, int actualQuantity, ProductModal productModal,
                                        Map<Integer, Object>  productMap) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("The actual quantity (" + actualQuantity +
                        ") does not match the order quantity (" + orderQuantity +
                        "). Do you still want to confirm?")
                .setPositiveButton("Yes", (dialog, which) ->
                        confirmProduct(productModal, productMap, actualQuantity))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateProductScanStatus(boolean isValid, String message) {
//        productBarcodeStatusOutboundIcon.setVisibility(View.VISIBLE);
//        productBarcodeStatusOutboundIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24
//                : R.drawable.baseline_cancel_24);
        productBarcodeStatusOutboundText.setVisibility(View.VISIBLE);
        productBarcodeStatusOutboundText.setText(message);
        productBarcodeStatusOutboundText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void updateWarehouseScanStatus(boolean isValid, String message) {
//        warehouseBarcodeStatusOutboundIcon.setVisibility(View.VISIBLE);
//        warehouseBarcodeStatusOutboundIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24
//                : R.drawable.baseline_cancel_24);
        warehouseBarcodeStatusOutboundText.setVisibility(View.VISIBLE);
        warehouseBarcodeStatusOutboundText.setText(message);
        warehouseBarcodeStatusOutboundText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    private void handleError(Throwable error) {
        String errorMsg = "An error occurred. Please try again.";
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = "Request timed out. Please check your connection.";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = "No internet connection!";
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }
}

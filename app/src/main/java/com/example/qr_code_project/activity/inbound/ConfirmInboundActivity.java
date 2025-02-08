package com.example.qr_code_project.activity.inbound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.SSLHelper;
import com.example.qr_code_project.ui.LoadingDialog;

import java.util.HashMap;
import java.util.Map;

public class ConfirmInboundActivity extends AppCompatActivity {

    private EditText barcodeEt, nameEt, quantityEt,
            realQuantityEt, warehouseCodeEt;
    private Button confirmBtn;
//    private ImageView productBarcodeStatusIcon, warehouseBarcodeStatusIcon;
    private TextView productBarcodeStatusText, warehouseBarcodeStatusText;

    private QRcodeManager qrCodeManager;

    private String scannedProductBarcode = "";
    private String scannedWarehouseBarcode = "";
    private boolean isConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_inbound);

        // Initialize UI components
        util();
        SSLHelper.trustAllCertificates();

        // Get data from InboundActivity
        getDataFromIntent();
    }

    private void getDataFromIntent() {
        ProductModal productModal = (ProductModal) getIntent().getSerializableExtra("product");
        if (productModal == null) {
            Toast.makeText(this, "Product data is missing!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Map<Integer, Object>  productMap =
                (Map<Integer, Object> ) getIntent().getSerializableExtra("productMap");
        if (productMap == null) {
            productMap = new HashMap<>();
        }

        // Show product information
        barcodeEt.setText(productModal.getCode());
        nameEt.setText(productModal.getTitle());
        quantityEt.setText(String.valueOf(productModal.getQuantity()));
        warehouseCodeEt.setText(productModal.getLocation());

        // Create QRCodeManager
        qrCodeManager = new QRcodeManager(this);
        qrCodeManager.setListener(this::handleScannedData);

        // Process when click submit
        Map<Integer, Object> finalProductMap = productMap;
        confirmBtn.setOnClickListener(v -> handleConfirmation(productModal, finalProductMap));
    }

    //UI component
    private void util(){
        barcodeEt = findViewById(R.id.barcodeEt);
        nameEt = findViewById(R.id.nameEt);
        quantityEt = findViewById(R.id.quantityEt);
        realQuantityEt = findViewById(R.id.realQuantityEt);
        warehouseCodeEt = findViewById(R.id.warehouseCodeEt);
        confirmBtn = findViewById(R.id.confirmBtn);
//        productBarcodeStatusIcon = findViewById(R.id.productBarcodeStatusIcon);
//        warehouseBarcodeStatusIcon = findViewById(R.id.warehouseBarcodeStatusIcon);
        productBarcodeStatusText = findViewById(R.id.productBarcodeStatusText);
        warehouseBarcodeStatusText = findViewById(R.id.warehouseBarcodeStatusText);

        confirmBtn.setVisibility(View.GONE);
    }

    //Check scan product and warehouse
    private void handleScannedData(String qrCodeText) {
        if (isConfirmed) return;

        if (scannedProductBarcode.isEmpty()) {
            scannedProductBarcode = qrCodeText;
            if (scannedProductBarcode.equals(barcodeEt.getText().toString().trim())) {
                updateProductScanStatus(true, "Product barcode is valid.");
            } else {
                updateProductScanStatus(false, "Invalid product barcode! Please scan again.");
                scannedProductBarcode = "";
            }
        } else if (scannedWarehouseBarcode.isEmpty()) {
            scannedWarehouseBarcode = qrCodeText;
            if (scannedWarehouseBarcode.equals(warehouseCodeEt.getText().toString().trim())) {
                updateWarehouseScanStatus(true, "Warehouse barcode is valid.");
                confirmBtn.setVisibility(View.VISIBLE);
//                if(qrCodeManager != null){
//                    qrCodeManager.unregister();
//                }
            } else {
                updateWarehouseScanStatus(false, "Invalid warehouse barcode! Please scan again.");
                scannedWarehouseBarcode = "";
                confirmBtn.setVisibility(View.GONE);
            }
        }
    }

    //Check real quantity
    private void handleConfirmation(ProductModal productModal, Map<Integer, Object> productMap) {
        String orderQuantityStr = quantityEt.getText().toString().trim();
        String realQuantityStr = realQuantityEt.getText().toString().replaceAll("[^0-9]", "");

        if (realQuantityStr.isEmpty()) {
            Toast.makeText(this, "Please enter actual quantity is number!", Toast.LENGTH_SHORT).show();
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

    //UI product scan
    private void updateProductScanStatus(boolean isValid, String message) {
//        productBarcodeStatusIcon.setVisibility(View.VISIBLE);
//        productBarcodeStatusIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24 : R.drawable.baseline_cancel_24);
        productBarcodeStatusText.setVisibility(View.VISIBLE);
        productBarcodeStatusText.setText(message);
        productBarcodeStatusText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    //UI warehouse scan
    private void updateWarehouseScanStatus(boolean isValid, String message) {
//        warehouseBarcodeStatusIcon.setVisibility(View.VISIBLE);
//        warehouseBarcodeStatusIcon.setImageResource(isValid ? R.drawable.baseline_gpp_good_24 : R.drawable.baseline_cancel_24);
        warehouseBarcodeStatusText.setVisibility(View.VISIBLE);
        warehouseBarcodeStatusText.setText(message);
        warehouseBarcodeStatusText.setTextColor(isValid ? Color.GREEN : Color.RED);
    }

    //Confirm product
    private void confirmProduct(ProductModal productModal, Map<Integer, Object> productMap, int actualQuantity) {
        isConfirmed = true;
        confirmBtn.setEnabled(false);

        Map<String, Object> productInfo = new HashMap<>();
        productInfo.put("actualQuantity", actualQuantity);
        productInfo.put("code", 0);
        productInfo.put("areaId", 0);
        productInfo.put("location", 0);

        productMap.put(productModal.getId(), productInfo);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("productMap", new HashMap<>(productMap));
        setResult(RESULT_OK, resultIntent);
        finish();

        Toast.makeText(this, "Product confirmed successfully!", Toast.LENGTH_SHORT).show();
    }

    //Show warning when quantity and real quantity don't same
    private void showConfirmationDialog(int orderQuantity, int actualQuantity,
                                        ProductModal productModal,
                                        Map<Integer, Object> productMap) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("The actual quantity (" + actualQuantity +
                        ") does not match the order quantity (" + orderQuantity +
                        "). Do you still want to confirm?")
                .setPositiveButton("Yes", (dialog, which) -> confirmProduct(productModal, productMap, actualQuantity))
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    realQuantityEt.setText("");
                })
                .show();
    }

    //Scan service
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeManager != null) {
            qrCodeManager.unregister();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrCodeManager.setListener(null);
    }

}

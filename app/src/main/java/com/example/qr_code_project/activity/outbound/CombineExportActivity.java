package com.example.qr_code_project.activity.outbound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ExportModal;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CombineExportActivity extends AppCompatActivity {

    private EditText totalExportCombineEt,totalProductCombineEt,
            totalRealQuantityCombineEt;
    private Button submitOutboundBtn;
    private RecyclerView combinedExportsRv;
    private ProductAdapter productAdapter;
    private SharedPreferences sharedPreferences;
    private final Map<Integer, Object> realQuantitiesMap = new HashMap<>();
    private ArrayList<ExportModal> deliveryList = new ArrayList<>();
    private ArrayList<ProductModal> productList = new ArrayList<>();
    private final Map<Integer, ArrayList<ProductModal>> deliveryProductsMap = new HashMap<>();
    //check all request update confirm
    private int pendingRequests = 0;
    private int successfulRequests = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_combine_export);

        util();

        productList = new ArrayList<>();
        deliveryList = new ArrayList<>();

        // Take a list export from Intent
        ArrayList<ExportModal> exportList = getIntent().getParcelableArrayListExtra("exportList");

        if (exportList != null && !exportList.isEmpty()) {
            deliveryList.addAll(exportList);
            totalExportCombineEt.setText(String.valueOf(exportList.size()));

            for (ExportModal export : exportList) {
                loadProductExport(export.getCodeEp());
                totalProductCombineEt.setText(String.valueOf(productList.size()));
            }
        } else {
            Toast.makeText(this, "Export list is empty!", Toast.LENGTH_SHORT).show();
            finish();
        }

        submitOutboundBtn.setOnClickListener(v -> {
            // Check all of deliveries
            for (ExportModal export : deliveryList) {
                // Create arraylist for save products in delivery
                ArrayList<Map<String, Object>> productListForDelivery = new ArrayList<>();
                ArrayList<ProductModal> productsForThisDelivery = deliveryProductsMap.get(export.getId());

                // Check all of products
                if (productsForThisDelivery != null) {
                    for (ProductModal product : productsForThisDelivery) {
                        // Get location and area from realQuantitiesMap
                        Map<Integer, Object> realQuantityInfo = (Map<Integer, Object>) realQuantitiesMap.get(product.getId());

                        if (realQuantityInfo != null) {
                            Object location = realQuantityInfo.get("location");
                            Object area = realQuantityInfo.get("areaId");

                            if (location != null && area != null) {
                                Map<String, Object> productData = new HashMap<>();
                                productData.put("productDelivenote_id", 0);
                                productData.put("id_product", product.getId());
                                productData.put("quantity", product.getQuantity());
                                productData.put("location", location);
                                productData.put("area", area);

                                productListForDelivery.add(productData);
                                Log.d("CombineExportActivity", "Added product to request: " + productData);
                            }
                        }
                    }
                } else {
                    Log.e("CombineExportActivity", "No products found for delivery " + export.getId());
                }

                // Create object JSON for send API
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("code", export.getCodeEp());
                requestData.put("id", export.getId());
                requestData.put("products", productListForDelivery);

                // Send request API for update data
                sendUpdateRequest(requestData);
            }
        });

    }

    //
    private void sendUpdateRequest(Map<String, Object> requestData) {
        String url = ApiConstants.UPDATE_DELIVERY_URL;
        JSONObject jsonObject = new JSONObject(requestData);
        Log.d("CombineExportActivity", "Sending update request: " + jsonObject.toString());

        pendingRequests++; //Cout request pending

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, jsonObject,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            successfulRequests++; // cout request success
                        } else {
                            Toast.makeText(CombineExportActivity.this, "Update fail!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(CombineExportActivity.this, "Server not response!", Toast.LENGTH_SHORT).show();
                    }
                    checkAllRequestsCompleted();
                },
                error -> {
                    Toast.makeText(CombineExportActivity.this, "Error send request!", Toast.LENGTH_SHORT).show();
                    checkAllRequestsCompleted();
                }) {
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

    private void checkAllRequestsCompleted() {
        pendingRequests--;

        if (pendingRequests == 0) { // Check all of request done
            if (successfulRequests > 0) {
                Toast.makeText(CombineExportActivity.this, "Update successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CombineExportActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CombineExportActivity.this, "Update fail all!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> confirmProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Map<Integer ,Object> returnedMap =
                            (HashMap<Integer,Object>) result.getData()
                                    .getSerializableExtra("realQuantitiesMap");

                    if (returnedMap != null) {
                        realQuantitiesMap.putAll(returnedMap);

                        if (productAdapter != null) {
                            productAdapter.notifyDataSetChanged();

                            int totalRealQuantity = 0;
                            for (Map.Entry<Integer, Object> entry : realQuantitiesMap.entrySet()) {
                                if (entry.getValue() instanceof Map) {
                                    Map<String, Object> info = (Map<String, Object>) entry.getValue();
                                    Object value = info.get("actualQuantity");

                                    if (value instanceof Number) {
                                        totalRealQuantity += ((Number) value).intValue();
                                    } else {
                                        Log.e("CombineExportActivity"
                                                , "actualQuantity is not a number: " + value);
                                    }
                                }
                            }
                            totalRealQuantityCombineEt.setText(String.valueOf(totalRealQuantity));
                        }
                    }

                }
            });

    private void loadProductExport(String codeEp) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(codeEp);

        @SuppressLint("NotifyDataSetChanged")
        StringRequest request = new StringRequest(Request.Method.GET, url, response -> {
            try {
                JSONObject jsonObject = new JSONObject(response);
                if (jsonObject.getBoolean("success")) {
                    JSONObject content = jsonObject.optJSONObject("content");
                    if (content != null) {
                        int delivery = content.optInt("id",0);
                        int totalProduct = 0;
                        JSONArray products = content.optJSONArray("products");

                        if (products != null) {
                            for (int i = 0; i < products.length(); i++) {
                                JSONObject object = products.getJSONObject(i);
                                int id = object.optInt("id");
                                String title = object.optString("title", "N/A");
                                int quantity = object.optInt("quantityDelivery");
                                totalProduct += quantity;
                                String code = object.optString("code", "N/A");
                                String image = object.optString("image", "");

                                boolean found = false;
                                for (ProductModal product : productList) {
                                    if (product.getId() == id) {
                                        product.setQuantity(product.getQuantity() + quantity);
                                        found = true;
                                        break;
                                    }
                                }

                                if (!found) {
                                    ProductModal newProduct = new ProductModal(
                                            id,
                                            title,
                                            quantity,
                                            null,
                                            code,
                                            image
                                    );
                                    totalProductCombineEt.setText(String.valueOf(totalProduct));
                                    productList.add(newProduct);
//                                    deliveryProductsMap.get(delivery).add(newProduct);
//                                    Log.d("CombineExportActivity", "Products for delivery " + delivery + ": " + deliveryProductsMap.get(delivery));

                                }
                                ArrayList<ProductModal> productListForThisDelivery = new ArrayList<>(productList);
                                deliveryProductsMap.put(delivery, productListForThisDelivery);
                                Log.d("CombineExportActivity", "Mapped products for delivery " + delivery + ": " + productListForThisDelivery);

                            }

                            if (productAdapter == null) {
                                productAdapter = new ProductAdapter(this, productList, realQuantitiesMap,
                                        (product, updatedMap) -> {
                                            Intent intent = new Intent(
                                                    CombineExportActivity.this,
                                                    ConfirmOutboundActivity.class);
                                            intent.putExtra("product", product);
                                            intent.putExtra("realQuantitiesMap",
                                                    new HashMap<>((Map) updatedMap));
                                            confirmProductLauncher.launch(intent);
                                        });

                                combinedExportsRv.setAdapter(productAdapter);
                            } else {
                                productAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(this, "Failed to parse product data!", Toast.LENGTH_SHORT).show();
            }
        }, error -> Toast.makeText(this, "Failed to load products!", Toast.LENGTH_SHORT).show()) {
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

    private void util(){
        totalExportCombineEt = findViewById(R.id.totalExportCombineEt);
        totalProductCombineEt = findViewById(R.id.totalProductCombineEt);
        totalRealQuantityCombineEt = findViewById(R.id.totalRealQuantityCombineEt);
        combinedExportsRv = findViewById(R.id.productExportRv);
        submitOutboundBtn = findViewById(R.id.submitOutboundBtn);

        combinedExportsRv.setLayoutManager(new LinearLayoutManager(this));
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
    }
}
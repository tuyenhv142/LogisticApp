package com.example.qr_code_project.activity.inbound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.QRcodeManager;
import com.example.qr_code_project.R;
import com.example.qr_code_project.activity.MainActivity;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.network.ApiService;
import com.example.qr_code_project.network.SSLHelper;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;
import com.google.android.material.textview.MaterialTextView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InboundActivity extends AppCompatActivity {

    private static final String TAG = "InboundActivity";

    // Create UI Components
    private EditText codeEt, titleEt;
    private TextView itemsEt, totalEt, totalRealQuantityEt;
    private RecyclerView productsRv;
    private Button submitBtn,resetBtn;

    // API and Data Management
    private int totalRealQuantity;
    private QRcodeManager qrcodeManager;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private ArrayList<ProductModal> productArrayList;
    private ProductAdapter productAdapter;
    private final Map<Integer, Object> productMap = new HashMap<>();
    private ApiService apiService;
    private LoadingDialog loadingDialog;
    private boolean isSubmit = false;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inbound);

        // Initialize UI components
        initUI();

        SSLHelper.trustAllCertificates();

        //Set event when click submit button
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = codeEt.getText().toString();
                if(totalRealQuantity == 0){
                    Toast.makeText(InboundActivity.this,"Real quantity is  null"
                            ,Toast.LENGTH_SHORT).show();
                    return;
                }
                submit(code,totalRealQuantity);
            }
        });

        //Set event when click reset button
        resetBtn.setOnClickListener(v -> resetData());

        if(isSubmit){
            qrcodeManager.unregister();
        }
    }
    //Scan service
    @Override
    protected void onResume() {
        super.onResume();
        qrcodeManager.setListener(this::loadInbound);
    }

    @Override
    protected void onPause() {
        super.onPause();
        qrcodeManager.setListener(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrcodeManager != null) {
            qrcodeManager.unregister();
        }
    }

    //Get data from ConfirmInboundActivity
    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> confirmProductLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    updateRealQuantities(result.getData());
                }
            });

    //Update total real quantity
    @SuppressLint("NotifyDataSetChanged")
    private void updateRealQuantities(Intent data) {
        Map<Integer, Object> updatedQuantities =
                (Map<Integer, Object>) data.getSerializableExtra("productMap");

//        loadingDialog.show();
        if (updatedQuantities != null) {
            productMap.putAll(updatedQuantities);
            productAdapter.notifyDataSetChanged();

            totalRealQuantity = 0;
            for (Map.Entry<Integer, Object> entry : productMap.entrySet()) {
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
            totalRealQuantityEt.setText(String.valueOf(totalRealQuantity));
            checkAllProductsConfirmed();
        }
    }

    //Check product commit for submit data
    private void checkAllProductsConfirmed() {
        if(productMap.size() == productArrayList.size()){
            submitBtn.setVisibility( View.VISIBLE);
            resetBtn.setVisibility(View.GONE);
            isSubmit = true;
        }else {
            submitBtn.setVisibility( View.GONE);
            isSubmit = false;
        }

//        loadingDialog.dismiss();

    }

    //Reset data for scan again
    @SuppressLint("NotifyDataSetChanged")
    private void resetData() {
        codeEt.setText("");
        titleEt.setText("");
        itemsEt.setText("");
        totalEt.setText("");
        totalRealQuantityEt.setText("0");

        productArrayList.clear();
        productMap.clear();
        totalRealQuantity = 0;

        // Update RecyclerView
        if (productAdapter != null) {
            productAdapter.notifyDataSetChanged();
        }
        // Create scan
        qrcodeManager = new QRcodeManager(this);
        qrcodeManager.setListener(this::loadInbound);

        // Hide submit button
        submitBtn.setVisibility(View.GONE);

        Toast.makeText(this, "Data has been reset. Please scan again!", Toast.LENGTH_SHORT).show();
    }

    //Submit data
    private void submit(String code, int quantity) {
        apiService.submitInbound(code, quantity, new ApiService.ApiResponseListener() {
            @Override
            public void onSuccess(String response) {
                Toast.makeText(InboundActivity.this, response, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(InboundActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InboundActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //UI components
    private void initUI() {
        codeEt = findViewById(R.id.codeEt);
        titleEt = findViewById(R.id.titleEt);
        itemsEt = findViewById(R.id.itemsEt);
        totalEt = findViewById(R.id.totalEt);
        productsRv = findViewById(R.id.productsRv);
        totalRealQuantityEt = findViewById(R.id.totalRealQuantityEt);
        submitBtn = findViewById(R.id.submitBtn);
        submitBtn.setVisibility(View.GONE);
        resetBtn = findViewById(R.id.resetBtn);

        qrcodeManager = new QRcodeManager(this);
        loadingDialog = new LoadingDialog(this);
        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        productsRv.setLayoutManager(new LinearLayoutManager(this));
        productArrayList = new ArrayList<>();
        apiService = new ApiService(this);
        tokenManager = new TokenManager(this);
        if(productMap.isEmpty()){
            totalRealQuantityEt.setText("0");
        }
    }

    //Get data Inbound from Api
    private void loadInbound(String scanValue) {
        if (scanValue == null || scanValue.isEmpty()) {
            Toast.makeText(this, "Scan value is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadingDialog.show();

//        if (productMap.size() == productArrayList.size() && !productArrayList.isEmpty()) {
//            qrcodeManager.unregister();
//        }

        String url = ApiConstants.getFindOneCodeInboundUrl(scanValue);
        StringRequest findInbound = new StringRequest(
                Request.Method.GET, url,
                this::parseResponse,
                this::handleError
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", null);
                if (tokenManager.isTokenExpired()) {
                    tokenManager.clearTokenAndLogout();
                }else {
                    headers.put("Authorization", "Bearer " + token);
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(findInbound);

    }

    //Process data from Api
    @SuppressLint("NotifyDataSetChanged")
    private void parseResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                if (content != null) {
                    populateContent(content);
                    if (!productArrayList.isEmpty()) {
                        qrcodeManager.unregister();
                    }
                }
            } else {
                showError("Don't have data for this inbound barcode!");
                if (qrcodeManager != null) {
                    qrcodeManager.setListener(this::loadInbound);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON response", e);
            showError("Failed to load data!");
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateContent(JSONObject content) throws JSONException {
        codeEt.setText(content.optString("code", "N/A"));
        titleEt.setText(content.optString("tite", "N/A"));
        itemsEt.setText(String.valueOf(content.optInt("totalProduct", 0)));
        totalEt.setText(String.valueOf(content.optInt("totalQuanTity", 0)));

        JSONArray products = content.optJSONArray("products");
        productArrayList.clear();
        for (int i = 0; i < Objects.requireNonNull(products).length(); i++) {
            JSONObject object = products.getJSONObject(i);

            int id = object.optInt("id");
            String title = object.optString("title", "N/A");
            int quantity = object.optInt("quantityImportFrom");
            String location = object.getJSONObject("dataItem")
                    .optString("code",null);
            String code = object.optString("code", "N/A");
            String image = object.optString("category_image", "N/A");

            productArrayList.add(new ProductModal(id, title, quantity, location, code, image));
        }

        if (productAdapter == null) {
            productAdapter = new ProductAdapter(this, productArrayList, productMap,
                    (product, updatedMap) -> {
                        Intent intent = new Intent(InboundActivity.this, ConfirmInboundActivity.class);
                        intent.putExtra("product", product);
                        intent.putExtra("productMap", new HashMap<>((Map) updatedMap));
                        confirmProductLauncher.launch(intent);
                    });

            productsRv.setAdapter(productAdapter);
        } else {
//            productAdapter.notifyItemRangeRemoved(0, productArrayList.size());
            productAdapter.notifyDataSetChanged();
        }
    }

    //Show error
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //Show error process Api
    private void handleError(Exception error) {
        String errorMsg = "An error occurred. Please try again.";
        if (error instanceof com.android.volley.TimeoutError) {
            errorMsg = "Request timed out. Please check your connection.";
        } else if (error instanceof com.android.volley.NoConnectionError) {
            errorMsg = "No internet connection!";
        }
        Log.e("API Error", error.getMessage(), error);
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
        loadingDialog.dismiss();
        if (qrcodeManager != null) {
            qrcodeManager.setListener(this::loadInbound);
        }
    }

}

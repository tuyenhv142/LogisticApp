package com.example.qr_code_project.activity.outbound;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qr_code_project.R;
import com.example.qr_code_project.adapter.ProductAdapter;
import com.example.qr_code_project.modal.ProductModal;
import com.example.qr_code_project.network.ApiConstants;
import com.example.qr_code_project.service.TokenManager;
import com.example.qr_code_project.ui.LoadingDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ExportDetailActivity extends AppCompatActivity {

//    private EditText codeExportEt,titleExportEt,itemsExportEt,totalExportEt;
    private Button returnBtn;
    private RecyclerView productExportRv;
    private ArrayList<ProductModal> productList;
    private SharedPreferences sharedPreferences;
    private LoadingDialog loadingDialog;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_export_detail);

        utils();

        String codeEp = getIntent().getStringExtra("codeEp");

        loadProductsForOrder(codeEp);

        utilButton();

    }

    private void utilButton() {
        returnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void loadProductsForOrder(String codeEp) {
        String url = ApiConstants.getFindOneCodeDeliveryUrl(codeEp);

        loadingDialog.show();
        @SuppressLint("NotifyDataSetChanged") StringRequest request = new StringRequest(
                Request.Method.GET, url,
                this::responseData,
                this::handleError
        )
        {
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

        Volley.newRequestQueue(this).add(request);
    }

    private void responseData(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.getBoolean("success")) {
                JSONObject content = jsonObject.optJSONObject("content");
                assert content != null;
                contentProduct(content);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }finally {
            loadingDialog.dismiss();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void contentProduct(JSONObject content) throws JSONException {
//                    String code = content.optString("code","N/A");
//                    String titleEx = content.optString("title","N/A");
//                    int items = content.optInt("totalProduct",0);
//                    int totalProduct = content.optInt("totalQuantity",0);
//
//
//                    codeExportEt.setText(code);
//                    titleExportEt.setText(titleEx);
//                    itemsExportEt.setText(String.valueOf(items));
//                    totalExportEt.setText(String.valueOf(totalProduct));
        JSONArray products = content.optJSONArray("products");
        if (products != null) {
            for (int i = 0; i < products.length(); i++) {
                JSONObject object = products.getJSONObject(i);
                int id = object.optInt("id");
                String title = object.optString("title", "N/A");
                int quantity = object.optInt("quantityDelivery");
//                String location = null;
                String code = object.optString("code");
                String image = object.optString("image", "");

                productList.add(new ProductModal(id, title, quantity, null, code, image));
            }
            ProductAdapter productAdapter = new ProductAdapter(this, productList, new HashMap<>(),
                    (product, updatedMap) -> Log.d("ExportDetailActivity", product.getTitle()));

            productExportRv.setAdapter(productAdapter);
            productAdapter.notifyDataSetChanged();
        }
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

    private void utils(){
//        codeExportEt = findViewById(R.id.codeExportEt);
//        titleExportEt = findViewById(R.id.titleExportEt);
//        itemsExportEt = findViewById(R.id.itemsExportEt);
//        totalExportEt = findViewById(R.id.totalExportEt);
        returnBtn = findViewById(R.id.returnBtn);
        productExportRv = findViewById(R.id.productExportRv);
        sharedPreferences = getSharedPreferences("AccountToken", MODE_PRIVATE);
        productExportRv.setLayoutManager(new LinearLayoutManager(this));
        loadingDialog = new LoadingDialog(this);
        productList = new ArrayList<>();
        tokenManager = new TokenManager(this);
    }
}
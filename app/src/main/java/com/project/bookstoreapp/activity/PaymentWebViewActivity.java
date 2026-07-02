package com.project.bookstoreapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.project.bookstoreapp.R;

public class PaymentWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_PAYMENT_URL = "extra_payment_url";
    private static final String RETURN_URL_PREFIX = "http://10.0.2.2:3000/api/vnpay_return";

    private WebView webViewPayment;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_web_view);

        MaterialToolbar toolbar = findViewById(R.id.toolbarPayment);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        webViewPayment = findViewById(R.id.webViewPayment);
        
        // Cấu hình WebView
        WebSettings webSettings = webViewPayment.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webViewPayment.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Nếu URL chuyển hướng về Return URL của hệ thống
                if (url.startsWith(RETURN_URL_PREFIX)) {
                    Uri uri = Uri.parse(url);
                    String responseCode = uri.getQueryParameter("vnp_ResponseCode");

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("vnp_ResponseCode", responseCode);
                    
                    if ("00".equals(responseCode)) {
                        setResult(RESULT_OK, resultIntent);
                    } else {
                        setResult(RESULT_CANCELED, resultIntent);
                    }
                    finish();
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });

        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        if (paymentUrl != null && !paymentUrl.isEmpty()) {
            webViewPayment.loadUrl(paymentUrl);
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.example.petshop.view.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petshop.R;
import com.example.petshop.utils.Constants;
import com.example.petshop.view.dialog.ConfirmDialog;
import com.example.petshop.view.dialog.DialogUtils;

public class VNPayWebViewActivity extends AppCompatActivity {

    public static final String EXTRA_PAYMENT_URL = "payment_url";
    public static final String EXTRA_ORDER_ID    = "order_id";

    // VNPay sẽ redirect đến VNPAY_RETURN_URL sau khi xử lý — ta intercept trước khi load
    private static final String RETURN_HOST = "petshop-payment.web.app";
    private static final String RETURN_PATH = "/vnpay-return";

    private WebView    webView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_webview);

        String paymentUrl = getIntent().getStringExtra(EXTRA_PAYMENT_URL);
        if (paymentUrl == null) { finish(); return; }

        progressBar = findViewById(R.id.webProgress);
        webView     = findViewById(R.id.webView);

        setupWebView();
        webView.loadUrl(paymentUrl);

        findViewById(R.id.btnClose).setOnClickListener(v -> confirmClose());
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return interceptReturnUrl(url);
            }

            // Hỗ trợ API cũ hơn
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return interceptReturnUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Intercept URL khi VNPay redirect về return URL.
     * Trả về true = ta xử lý, không để WebView load.
     */
    private boolean interceptReturnUrl(String url) {
        if (url == null) return false;

        // Kiểm tra URL có khớp với return URL pattern không
        if (url.contains(RETURN_HOST + RETURN_PATH) || url.startsWith(Constants.VNPAY_RETURN_URL)) {
            handlePaymentResult(url);
            return true;
        }
        return false;
    }

    private void handlePaymentResult(String url) {
        try {
            Uri uri = Uri.parse(url);
            String responseCode = uri.getQueryParameter("vnp_ResponseCode");
            String orderId      = uri.getQueryParameter("vnp_TxnRef");
            boolean isSuccess   = "00".equals(responseCode);

            Intent intent = new Intent(this, VNPayResultActivity.class);
            // Build deep link URI từ params đã có
            Uri resultUri = Uri.parse("petshop://payment/vnpay-return")
                    .buildUpon()
                    .appendQueryParameter("vnp_ResponseCode", responseCode != null ? responseCode : "")
                    .appendQueryParameter("vnp_TxnRef", orderId != null ? orderId : "")
                    .build();
            intent.setData(resultUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            finish();
        }
    }

    private void confirmClose() {
        DialogUtils.showConfirmDialog(this, "Bạn có muốn huỷ thanh toán?",
            new ConfirmDialog.OnConfirmListener() {
                @Override
                public void onConfirm() {
                    finish();
                }

                @Override
                public void onCancel() {
                    // Tiếp tục
                }
            });
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else confirmClose();
    }
}

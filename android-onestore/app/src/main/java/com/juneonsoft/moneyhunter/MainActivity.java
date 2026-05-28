package com.juneonsoft.moneyhunter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TARGET_PARAM = "target";
    private static final String TARGET_VALUE = "onestore";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(Color.rgb(15, 23, 42));
        setContentView(webView);
        configureSystemBars();
        configureWebView();
        loadGame();
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(Color.rgb(15, 23, 42));
        getWindow().setNavigationBarColor(Color.rgb(15, 23, 42));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        webView.setOnApplyWindowInsetsListener((view, insets) -> {
            applySystemBarPadding(view, insets);
            return insets;
        });
        webView.post(webView::requestApplyInsets);
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarPadding(View view, WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Insets systemBars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return;
        }
        view.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                Toast.makeText(MainActivity.this, "지원하지 않는 링크입니다.", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    return false;
                }
                Toast.makeText(MainActivity.this, "지원하지 않는 링크입니다.", Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showNetworkError();
                }
            }
        });
    }

    private void loadGame() {
        if (!isNetworkAvailable()) {
            showNetworkError();
            return;
        }
        webView.loadUrl(oneStoreUrl(BuildConfig.GAME_URL));
    }

    private String oneStoreUrl(String url) {
        Uri uri = Uri.parse(url);
        if (TARGET_VALUE.equalsIgnoreCase(uri.getQueryParameter(TARGET_PARAM))) {
            return url;
        }
        return uri.buildUpon()
                .appendQueryParameter(TARGET_PARAM, TARGET_VALUE)
                .build()
                .toString();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void showNetworkError() {
        String html = "<!doctype html><html lang=\"ko\"><head>"
                + "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>body{margin:0;height:100vh;display:grid;place-items:center;background:#0f172a;color:#f8fafc;font-family:sans-serif}"
                + "main{max-width:320px;padding:24px;text-align:center}h1{font-size:22px}p{line-height:1.5;color:#cbd5e1}"
                + "button{margin-top:14px;border:0;border-radius:10px;background:#facc15;color:#1f2937;padding:14px 18px;font-weight:900}</style>"
                + "</head><body><main><h1>게임을 불러오지 못했어요</h1>"
                + "<p>인터넷 연결을 확인한 뒤 다시 시도해 주세요.</p>"
                + "<button onclick=\"location.reload()\">다시 시도</button></main></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}

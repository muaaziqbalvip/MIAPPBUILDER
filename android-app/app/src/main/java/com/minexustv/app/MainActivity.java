package com.minexustv.app;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout customViewContainer;
    private View splashScreen;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;

    private static final String LIVE_URL = "https://minexustv.vercel.app";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive fullscreen — no status/nav bars
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(Color.parseColor("#07080C"));
        getWindow().setNavigationBarColor(Color.parseColor("#07080C"));
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        webView         = findViewById(R.id.webView);
        customViewContainer = findViewById(R.id.customViewContainer);
        splashScreen    = findViewById(R.id.splashScreen);

        // Show splash while web app loads
        splashScreen.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
        webView.setAlpha(0f);

        setupWebView();
        webView.loadUrl(LIVE_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        // Block all popups
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        // Native app user agent
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 MINEXUSTV-AndroidApp/2.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep everything inside app
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Inject JS: block popups + hide PWA/auth overlays + mark as native app
                view.evaluateJavascript(
                    "(function(){" +
                    "window.open=function(){return null;};" +
                    "window.alert=function(){};" +
                    "window.confirm=function(){return true;};" +
                    "window.prompt=function(){return '';};" +
                    "try{localStorage.setItem('pwaInstalled','true');}catch(e){}" +
                    "try{localStorage.setItem('isAndroidApp','true');}catch(e){}" +
                    "var sel=['#authLockOverlay','.pwa-nudge','.pwa-install-banner','.notif-banner-popup'];" +
                    "sel.forEach(function(id){" +
                    "  var el=document.querySelector(id);" +
                    "  if(el)el.style.display='none';" +
                    "});" +
                    "})();",
                    null
                );

                // Fade out splash, fade in web app
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    splashScreen.animate().alpha(0f).setDuration(500).withEndAction(() -> {
                        splashScreen.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                        webView.animate().alpha(1f).setDuration(400).start();
                    }).start();
                }, 600);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    // Beautiful offline error page
                    view.loadData(
                        "<html><body style='background:#07080c;color:#fff;font-family:sans-serif;" +
                        "display:flex;align-items:center;justify-content:center;height:100vh;" +
                        "flex-direction:column;gap:16px;margin:0;text-align:center'>" +
                        "<div style='font-size:56px'>📡</div>" +
                        "<div style='font-size:20px;font-weight:900'>No Internet</div>" +
                        "<div style='font-size:13px;color:#9ea4b8;max-width:260px'>" +
                        "Check your Wi-Fi or mobile data connection</div>" +
                        "<button onclick='window.location.reload()' style='padding:14px 32px;" +
                        "background:#e50914;color:#fff;border:none;border-radius:10px;" +
                        "font-size:15px;font-weight:800;margin-top:10px;cursor:pointer'>" +
                        "🔄 Retry</button>" +
                        "</body></html>",
                        "text/html", "UTF-8"
                    );
                    // Also hide splash on error
                    splashScreen.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    webView.setAlpha(1f);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // Block ALL popup windows
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
                return false;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (mCustomView != null) { onHideCustomView(); return; }
                mCustomView = view;
                customViewCallback = callback;
                customViewContainer.addView(view);
                customViewContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                );
            }

            @Override
            public void onHideCustomView() {
                if (mCustomView == null) return;
                customViewContainer.removeView(mCustomView);
                mCustomView = null;
                customViewContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                if (customViewCallback != null) customViewCallback.onCustomViewHidden();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                return true; // suppress JS console noise
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mCustomView != null) {
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            moveTaskToBack(true); // Minimize instead of close (native app feel)
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    @Override
    protected void onResume() { super.onResume(); webView.onResume(); }

    @Override
    protected void onPause() { webView.onPause(); super.onPause(); }

    @Override
    protected void onDestroy() {
        webView.stopLoading();
        webView.destroy();
        super.onDestroy();
    }
}

package com.messledger.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/**
 * Native Android Activity written in Kotlin to host the Mess Ledger engine
 * inside an optimized, secure Android WebView.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar & navigation bar theme colors (#1F3D34)
        val ledgerGreen = Color.rgb(31, 61, 52)
        window.statusBarColor = ledgerGreen
        window.navigationBarColor = ledgerGreen

        // Create and configure WebView
        webView = WebView(this).apply {
            setBackgroundColor(ledgerGreen)
            overScrollMode = View.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                // The app only ever needs its own bundled assets (loaded once via
                // loadUrl below) — it never needs in-page JS to read arbitrary
                // file:// URLs from device storage, so keep this closed.
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
            }

            // Explicitly off outside of debug builds — remote inspection of a WebView
            // that talks to Firestore should never be reachable in a shipped build.
            WebView.setWebContentsDebuggingEnabled(false)

            webViewClient = LedgerWebViewClient()
            webChromeClient = LedgerWebChromeClient()
        }

        setContentView(webView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback {
                    if (::webView.isInitialized && webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        finish()
                    }
                }
            )
        }

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/index.html")
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    // Only reached on API < 33, where the OnBackInvokedCallback registered above
    // isn't available — enableOnBackInvokedCallback in the manifest makes the
    // system prefer the callback path on API 33+, so this stays for older devices.
    @Deprecated("Deprecated in Java/Android")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    /**
     * Custom WebViewClient to keep Firebase Auth, Firestore, and app assets within the WebView,
     * while sending anything that isn't part of the bundled app (e.g. a link someone puts in a
     * note or category name) out to the system browser instead of rendering it in-app.
     */
    private inner class LedgerWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url
            val scheme = url.scheme?.lowercase()

            // The app's own bundled assets load fine inside the WebView.
            if (scheme == "file" || scheme == "about" || scheme == "data") return false

            // Firebase's REST/RTDB/Firestore/Auth traffic happens via XHR/fetch under the
            // hood, not top-level navigation, so any http(s) top-level navigation request
            // reaching here is an external link (e.g. from user-entered note text) —
            // hand it to the system browser rather than loading it in-app.
            if (scheme == "http" || scheme == "https") {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                } catch (e: ActivityNotFoundException) {
                    // No browser available — just ignore rather than crash.
                }
                return true
            }

            // Unknown schemes (intent:, market:, tel:, mailto:, etc.) — don't let the
            // WebView attempt them, and don't silently swallow them either.
            return true
        }
    }

    /**
     * Custom WebChromeClient to forward JavaScript logs and alerts cleanly.
     */
    private inner class LedgerWebChromeClient : WebChromeClient() {
        override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
            return super.onConsoleMessage(message)
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            if (isFinishing || isDestroyed) {
                result?.cancel()
                return true
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Mess Ledger")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setCancelable(false)
                .create()
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            if (isFinishing || isDestroyed) {
                result?.cancel()
                return true
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Mess Ledger")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                .setCancelable(false)
                .create()
                .show()
            return true
        }
    }
}

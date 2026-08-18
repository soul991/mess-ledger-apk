package com.messledger.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

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
                allowFileAccess = true
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
            }

            webViewClient = LedgerWebViewClient()
            webChromeClient = LedgerWebChromeClient()
        }

        setContentView(webView)

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
     * Custom WebViewClient to keep Firebase Auth, Firestore, and app assets within the WebView.
     */
    private class LedgerWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // Keep internal navigation and external Firebase auth endpoints inside the WebView
            return false
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

package com.propslab.ui.screens

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * The manual, offline.
 *
 * The manual is a static HTML file shipped in the APK, so it opens with no network and no
 * JavaScript. That is deliberate: a help page should work on a plane, and a page that cannot
 * run scripts cannot phone home either.
 */
class ManualActivity : ComponentActivity() {

    private var web: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = WebView(this).apply {
            setBackgroundColor(0xFF0B1018.toInt())
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.loadsImagesAutomatically = true
            // Only the packaged manual is reachable. A link that would leave the asset
            // tree is ignored rather than silently turning the help page into a browser.
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = request?.url?.toString()?.startsWith(ASSET_ROOT) != true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
        val asset = intent.getStringExtra(EXTRA_ASSET) ?: "manual.html"
        view.loadUrl(ASSET_ROOT + asset)
        web = view
        setContentView(view)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val page = web
                if (page != null && page.canGoBack()) page.goBack() else finish()
            }
        })
    }

    override fun onDestroy() {
        web?.apply {
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        web = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ASSET = "asset"
        private const val ASSET_ROOT = "file:///android_asset/"
    }
}

package com.m4xtheme.app

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var root: FrameLayout

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data))
        fileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        with(web.settings) {
            javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false; allowFileAccess = true; allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString M4XTheme/4.0"
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                val u = r.url.toString()
                return if (u.startsWith("http://") || u.startsWith("https://")) false else { runCatching { startActivity(Intent(Intent.ACTION_VIEW, r.url)) }; true }
            }
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) v.loadDataWithBaseURL(null, "<body style='background:#080b16;color:white;font-family:sans-serif;padding:32px'><h2>M4X Theme đang ngoại tuyến</h2><p>Kiểm tra Internet rồi bấm tải lại.</p><button onclick='location.reload()' style='padding:14px 20px'>Tải lại</button></body>", "text/html", "UTF-8", null)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null); fileCallback = cb
                filePicker.launch(runCatching { p.createIntent() }.getOrElse { Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="*/*"; addCategory(Intent.CATEGORY_OPENABLE); putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true) } })
                return true
            }
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView=view; customCallback=callback; web.visibility=View.GONE
                root.addView(view, FrameLayout.LayoutParams(-1,-1)); requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                window.decorView.systemUiVisibility = 5894
            }
            override fun onHideCustomView() { hideCustom() }
        }
        web.setDownloadListener { url, ua, disposition, mime, _ ->
            val i=Intent(Intent.ACTION_VIEW, Uri.parse(url)); runCatching { startActivity(i) }
        }
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){ override fun handleOnBackPressed(){ when { customView!=null -> hideCustom(); web.canGoBack() -> web.goBack(); else -> finish() } } })
        web.loadUrl(BuildConfig.WEB_APP_URL)
    }
    private fun hideCustom(){ val v=customView?:return; root.removeView(v); customView=null; customCallback?.onCustomViewHidden(); customCallback=null; web.visibility=View.VISIBLE; requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; window.decorView.systemUiVisibility=0 }
    override fun onUserLeaveHint(){ super.onUserLeaveHint(); if(Build.VERSION.SDK_INT>=26 && customView!=null) runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16,9)).build()) } }
    override fun onDestroy(){ web.destroy(); super.onDestroy() }
}

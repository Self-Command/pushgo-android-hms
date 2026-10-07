package io.ethan.pushgo.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import io.ethan.pushgo.ui.theme.PushGoTheme
import java.io.ByteArrayInputStream
import java.io.File

/** HTTPS origin-bound browser without a JavaScript bridge or native task-specific settings. */
class WebActionActivity : AppCompatActivity() {
    private var browser: WebView? = null
    private var chooser: ValueCallback<Array<Uri>>? = null
    private var cameraUri: Uri? = null
    private var loadProgress by mutableIntStateOf(0)
    private var pageError by mutableStateOf(false)
    private lateinit var origin: WebOrigin
    private var initialUrl = ""
    private var pageLabel = "打开页面"
    private val gallery = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val accepted = uri?.takeIf { it.scheme == "content" && it.authority != "$packageName.fileprovider" && runCatching { contentResolver.getType(it) in supportedImages }.getOrDefault(false) }
        if (uri != null && accepted == null) tell("请选择 JPEG、PNG 或 WebP 图片。")
        chooser?.onReceiveValue(accepted?.let { arrayOf(it) }); chooser = null
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val callback = chooser; chooser = null
        if (success && callback != null && cameraUri != null) callback.onReceiveValue(arrayOf(cameraUri!!))
        else { callback?.onReceiveValue(null); clearCamera(); if (success) tell("页面已恢复，请重新选择照片。") }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        origin = WebOrigin.parse(initialUrl) ?: run { finish(); return }
        pageLabel = intent.getStringExtra(EXTRA_LABEL)?.take(32) ?: "打开页面"
        cameraUri = savedInstanceState?.getString("camera_uri")?.let(Uri::parse)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (browser?.canGoBack() == true) browser?.goBack() else finish() }
        })
        setContent { PushGoTheme { Scaffold(topBar = { TopAppBar(title = { Column { Text(pageLabel, style = MaterialTheme.typography.titleMedium); Text(origin.host, style = MaterialTheme.typography.bodySmall) } }, navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回消息") } }) }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets)) {
                if (loadProgress < 100) LinearProgressIndicator()
                if (pageError) Text("页面暂时无法打开，请返回消息后重试。", modifier = Modifier.padding(insets))
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { createBrowser(it) })
            }
        } } }
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun createBrowser(context: Context): WebView = WebView(context).apply {
        browser = this
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (origin.permits(url)) return false
                if (request.isForMainFrame) WebOrigin.external(url)?.let { external -> runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(external))) } }
                return true
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return if (origin.permits(request.url.toString())) null else WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(ByteArray(0)))
            }
            override fun onPageStarted(view: WebView, url: String, icon: android.graphics.Bitmap?) { if (!origin.permits(url)) view.stopLoading(); pageError = false }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) { handler.cancel(); pageError = true }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) { if (request.isForMainFrame) pageError = true }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, value: Int) { loadProgress = value }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null); chooser = callback
                if (!origin.permits(view.url.orEmpty())) { chooser?.onReceiveValue(null); chooser = null; return true }
                val types = params.acceptTypes.flatMap { it.split(',') }.map { it.trim() }.filter { it.isNotEmpty() }
                if (types.isNotEmpty() && types.none { it in supportedImages || it == "image/*" || it == ".jpg" || it == ".jpeg" || it == ".png" || it == ".webp" }) { tell("此页面目前支持选择图片。"); chooser?.onReceiveValue(null); chooser = null; return true }
                clearCamera()
                runCatching {
                    if (params.isCaptureEnabled) {
                        val directory = File(cacheDir, "web-photos").apply { mkdirs() }
                        val file = File.createTempFile("capture-", ".jpg", directory)
                        cameraUri = FileProvider.getUriForFile(this@WebActionActivity, "$packageName.fileprovider", file)
                        camera.launch(cameraUri!!)
                    } else gallery.launch(supportedImages.toTypedArray())
                }.onFailure { chooser?.onReceiveValue(null); chooser = null; clearCamera(); tell("暂时无法选择照片，请重试。") }
                return true
            }
        }
        loadUrl(initialUrl)
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); cameraUri?.let { outState.putString("camera_uri", it.toString()) } }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig) }
    private fun clearCamera() {
        cameraUri?.let { uri ->
            runCatching { revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            val name = uri.lastPathSegment ?: return@let
            if (name.startsWith("capture-") && name.endsWith(".jpg")) File(File(cacheDir, "web-photos"), name).delete()
        }; cameraUri = null
    }
    private fun tell(message: String) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    override fun onDestroy() { chooser?.onReceiveValue(null); chooser = null; browser?.apply { stopLoading(); webChromeClient = null; destroy() }; browser = null; clearCamera(); super.onDestroy() }
    companion object {
        private const val EXTRA_URL = "web_action_url"
        private const val EXTRA_LABEL = "web_action_label"
        private val supportedImages = setOf("image/jpeg", "image/png", "image/webp")
        fun intent(context: Context, action: WebAction): Intent = Intent(context, WebActionActivity::class.java).putExtra(EXTRA_URL, action.url).putExtra(EXTRA_LABEL, action.label)
        fun open(context: Context, action: WebAction) { context.startActivity(intent(context, action)) }
    }
}

package im.miaoli.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Opens a tribune's login page in a WebView and returns its cookie header.
 *
 * The cookie is read for [cookieUrl] so it matches the actual post endpoint;
 * [loginUrl] is used as a fallback for read-only tribunes.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TribuneLoginScreen(
    title: String,
    loginUrl: String,
    cookieUrl: String,
    onCancel: () -> Unit,
    onCookies: (String) -> Unit,
) {
    val context = LocalContext.current
    val cookieManager = remember { CookieManager.getInstance() }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun navigateBack() {
        val view = webView
        if (view?.canGoBack() == true) {
            view.goBack()
        } else {
            onCancel()
        }
    }

    BackHandler {
        navigateBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            MiaoliTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            cookieManager.flush()
                            val cookies = cookieManager.getCookie(cookieUrl)
                                .orEmpty()
                                .ifBlank { cookieManager.getCookie(loginUrl).orEmpty() }
                            if (cookies.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "No cookies found. Finish logging in first.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                onCookies(cookies.trim())
                            }
                        },
                    ) {
                        Text("Done")
                    }
                },
            )
        },
    ) { innerPadding ->
        AndroidView(
            factory = { viewContext ->
                WebView(viewContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    loadUrl(loginUrl)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onRelease = { view ->
                if (webView === view) {
                    webView = null
                }
                view.destroy()
            },
        )
    }
}

package com.teyvatmap.ui

import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teyvatmap.R
import com.teyvatmap.data.CookieManager
import com.teyvatmap.data.CookieParser
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: MapViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val showCookieInput by remember { mutableStateOf(false) }
    val cookieText by remember { mutableStateOf("") }
    val isLoading by remember { mutableStateOf(false) }
    val errorMessage by remember { mutableStateOf<String?>(null) }

    // Observe cookie status
    val hasValidCookie by viewModel.hasValidCookie.collectAsStateWithLifecycle()
    val cookieStatus by viewModel.cookieStatus.collectAsStateWithLifecycle()

    // Auto-navigate if already logged in
    androidx.compose.runtime.LaunchedEffect(hasValidCookie) {
        if (hasValidCookie) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo/Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "Teyvat Map",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 8.dp))
            Text(
                "Interactive Genshin Impact Map",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 32.dp))

        // Login Options Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Sign in to sync your map progress",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 16.dp))

                // WebView Login Button
                Button(
                    onClick = { /* Handled by WebViewLoginScreen */ },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text("Continue with HoYoLAB")
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 12.dp))

                // Manual Cookie Input
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.ui.draw.Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "OR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 12.dp))

                Button(
                    onClick = { showCookieInput.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Paste Cookie Manually")
                    }
                }

                errorMessage?.let { msg ->
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.padding(top = 12.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }

        // WebView Login (hidden until clicked)
        if (!showCookieInput) {
            WebViewLogin(
                modifier = Modifier.fillMaxSize(),
                onCookieObtained = { cookie ->
                    viewModel.saveCookie(cookie)
                    onLoginSuccess()
                },
                onCancel = { }
            )
        }
    }

    // Manual Cookie Dialog
    if (showCookieInput) {
        ManualCookieDialog(
            onDismiss = { showCookieInput.value = false },
            onSave = { cookie ->
                viewModel.saveCookie(cookie)
                showCookieInput.value = false
            }
        )
    }
}

@Composable
fun WebViewLogin(
    modifier: Modifier,
    onCookieObtained: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val webViewClient = remember { LoginWebViewClient(onCookieObtained) }
    val webChromeClient = remember { LoginWebChromeClient() }
    val isLoading = remember { mutableStateOf(true) }
    val canGoBack = remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36"
                }
                webViewClient = webViewClient
                webChromeClient = webChromeClient
                loadUrl("https://www.hoyolab.com/account/login?redirect=https%3A%2F%2Fwww.hoyolab.com%2Fmap%2F")
            }
            webView
        },
        update = { webView ->
            webView.webViewClient = webViewClient
            webView.webChromeClient = webChromeClient
        }
    )

    // Top bar for WebView
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { Text("HoYoLAB Login") },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            IconButton(onClick = { /* refresh */ }, enabled = isLoading.value) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { /* go back */ }, enabled = canGoBack.value) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

class LoginWebViewClient(
    private val onCookieObtained: (String) -> Unit
) : WebViewClient() {

    private var cookieObtained = false

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        // Check if we're on the map page (successful login)
        if (url.contains("hoyolab.com/map") || url.contains("act.hoyolab.com/map")) {
            extractCookies(view)
            return true
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        // Extract cookies after page load
        if (url.contains("hoyolab.com") && !cookieObtained) {
            extractCookies(view)
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.proceed()
    }

    private fun extractCookies(view: WebView) {
        val cookieManager = CookieManager.getInstance()
        val cookie = cookieManager.getCookie("https://www.hoyolab.com") ?: ""
            .also { cookieManager.getCookie("https://act.hoyolab.com")?.let { cookie += "; $it" } }
            .also { cookieManager.getCookie("https://api-takumi.mihoyo.com")?.let { cookie += "; $it" } }

        if (cookie.isNotBlank() && CookieParser.hasValidTokens(cookie)) {
            cookieObtained = true
            onCookieObtained(cookie)
        }
    }
}

class LoginWebChromeClient : WebChromeClient() {
    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        // Could update loading indicator
    }
}

@Composable
fun ManualCookieDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var cookieText by androidx.compose.runtime.mutableStateOf("")
    val errorMessage = androidx.compose.runtime.mutableStateOf<String?>(null)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste HoYoLAB Cookie") },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(400.dp)) {
                Text(
                    "Get your cookie from browser dev tools (F12) → Application → Cookies → hoyolab.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
                androidx.compose.material3.TextField(
                    value = cookieText,
                    onValueChange = { cookieText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    label = { Text("Cookie (Netscape/JSON format)") },
                    singleLine = false,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default,
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                errorMessage.value?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (cookieText.isNotBlank()) {
                        onSave(cookieText)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
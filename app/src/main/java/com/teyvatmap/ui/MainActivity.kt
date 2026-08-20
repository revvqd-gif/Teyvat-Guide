package com.teyvatmap.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teyvatmap.R
import com.teyvatmap.data.MapRepository
import com.teyvatmap.data.CookieManager
import com.teyvatmap.TeyvatMapApplication
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MapViewModel by viewModels {
        TeyvatMapApplication.getInstance().repository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeyvatMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainNavHost(viewModel: MapViewModel) {
    var isLoggedIn by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val hasValidCookie by viewModel.hasValidCookie.collectAsStateWithLifecycle()

    // Auto-update login state
    androidx.compose.runtime.LaunchedEffect(hasValidCookie) {
        isLoggedIn = hasValidCookie
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoggedIn) {
            MapScreen(
                viewModel = viewModel,
                onLogout = {
                    isLoggedIn = false
                }
            )
        } else {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    isLoggedIn = true
                }
            )
        }
    }
}
package com.example.cherrycharm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.notifications.NotificationHelper
import com.example.cherrycharm.ui.MainViewModel
import com.example.cherrycharm.ui.navigation.AppNavHost
import com.example.cherrycharm.ui.theme.CherryCharmTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val pendingDeepLink = MutableStateFlow<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        NotificationHelper.createChannels(this)
        askForNotificationPermission()
        pendingDeepLink.value = intent.deepLink()

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val deepLink by pendingDeepLink.collectAsStateWithLifecycle()
            CherryCharmTheme(themeMode = themeMode) {
                AppNavHost(
                    pendingDeepLink = deepLink,
                    onDeepLinkHandled = { pendingDeepLink.value = null },
                    mainViewModel = mainViewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.deepLink()?.let { pendingDeepLink.value = it }
    }

    private fun Intent?.deepLink(): String? =
        this?.getStringExtra(Constants.DEEP_LINK_EXTRA) ?: this?.data?.toString()

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

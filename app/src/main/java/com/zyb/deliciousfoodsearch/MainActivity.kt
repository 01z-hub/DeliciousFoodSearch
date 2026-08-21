package com.zyb.deliciousfoodsearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.zyb.deliciousfoodsearch.data.AmapFoodDataSource
import com.zyb.deliciousfoodsearch.data.AmapPrivacyConsent
import com.zyb.deliciousfoodsearch.data.FoodDiscoveryRepository
import com.zyb.deliciousfoodsearch.data.SharedPreferencesRestaurantHistoryStore
import com.zyb.deliciousfoodsearch.location.SystemSingleLocationProvider
import com.zyb.deliciousfoodsearch.ui.DeliciousFoodSearchApp
import com.zyb.deliciousfoodsearch.ui.FoodDiscoveryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val historyStore by lazy { SharedPreferencesRestaurantHistoryStore(this) }
    private val locationProvider by lazy { SystemSingleLocationProvider(this) }
    private val privacyPreferences by lazy {
        getSharedPreferences(PRIVACY_PREFERENCES, MODE_PRIVATE)
    }

    private val viewModel: FoodDiscoveryViewModel by viewModels {
        FoodDiscoveryViewModel.Factory(
            FoodDiscoveryRepository(
                dataSource = AmapFoodDataSource(applicationContext),
                historyStore = historyStore,
            ),
        )
    }

    private var privacyAccepted by mutableStateOf(false)
    private var amapReady by mutableStateOf(false)
    private var isPreparingAmap by mutableStateOf(false)
    private var showPrivacyDialog by mutableStateOf(false)

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                loadCurrentLocation()
            } else {
                viewModel.reportLocationPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        privacyAccepted = privacyPreferences.getBoolean(KEY_AMAP_PRIVACY_ACCEPTED, false)
        showPrivacyDialog = !privacyAccepted
        setContent {
            DeliciousFoodSearchApp(
                viewModel = viewModel,
                privacyAccepted = privacyAccepted,
                amapReady = amapReady,
                showPrivacyDialog = showPrivacyDialog,
                onRequestPrivacyConsent = { showPrivacyDialog = true },
                onAcceptPrivacy = ::acceptPrivacy,
                onDeclinePrivacy = ::declinePrivacy,
                onOpenPrivacyPolicy = {
                    openWebPage("https://lbs.amap.com/pages/privacy/")
                },
                onRequestLocation = ::requestLocation,
                onUseBeijingCenter = viewModel::useBeijingCenter,
                onOpenMeituan = { restaurantName ->
                    openWebPage("https://www.meituan.com/s/${Uri.encode(restaurantName)}/")
                },
                onOpenDouyin = { restaurantName ->
                    openWebPage("https://www.douyin.com/search/${Uri.encode(restaurantName)}")
                },
            )
        }

        if (privacyAccepted) {
            prepareAmapAndRequestLocation()
        }
    }

    private fun acceptPrivacy() {
        privacyPreferences.edit().putBoolean(KEY_AMAP_PRIVACY_ACCEPTED, true).apply()
        privacyAccepted = true
        showPrivacyDialog = false
        prepareAmapAndRequestLocation()
    }

    private fun declinePrivacy() {
        privacyPreferences.edit().putBoolean(KEY_AMAP_PRIVACY_ACCEPTED, false).apply()
        privacyAccepted = false
        amapReady = false
        showPrivacyDialog = false
        lifecycleScope.launch(Dispatchers.Default) {
            AmapPrivacyConsent.update(this@MainActivity, agreed = false)
        }
    }

    private fun prepareAmapAndRequestLocation() {
        if (isPreparingAmap || amapReady) {
            if (amapReady) requestLocation()
            return
        }
        isPreparingAmap = true
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Default) {
                    AmapPrivacyConsent.update(this@MainActivity, agreed = true)
                }
                amapReady = true
                requestLocation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                amapReady = false
                viewModel.reportAmapInitializationUnavailable()
            } finally {
                isPreparingAmap = false
            }
        }
    }

    private fun requestLocation() {
        if (!privacyAccepted) {
            showPrivacyDialog = true
            return
        }
        if (!amapReady) {
            prepareAmapAndRequestLocation()
            return
        }
        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            loadCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    private fun loadCurrentLocation() {
        viewModel.reportLocationStarted()
        locationProvider.requestLocation(
            onSuccess = { result ->
                viewModel.useDeviceLocation(
                    location = result.point,
                    accuracyMeters = result.accuracyMeters,
                )
            },
            onFailure = viewModel::reportLocationUnavailable,
        )
    }

    private fun openWebPage(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroy() {
        locationProvider.cancel()
        super.onDestroy()
    }

    companion object {
        private const val PRIVACY_PREFERENCES = "privacy_consent"
        private const val KEY_AMAP_PRIVACY_ACCEPTED = "amap_privacy_accepted"
    }
}

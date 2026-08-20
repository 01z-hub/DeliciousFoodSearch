package com.zyb.deliciousfoodsearch

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.zyb.deliciousfoodsearch.data.DemoFoodDataSource
import com.zyb.deliciousfoodsearch.data.FoodDiscoveryRepository
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.ui.DeliciousFoodSearchApp
import com.zyb.deliciousfoodsearch.ui.FoodDiscoveryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FoodDiscoveryViewModel by viewModels {
        FoodDiscoveryViewModel.Factory(
            FoodDiscoveryRepository(DemoFoodDataSource()),
        )
    }

    private var locationCancellationSignal: CancellationSignal? = null

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
        setContent {
            DeliciousFoodSearchApp(
                viewModel = viewModel,
                onRequestLocation = ::requestLocation,
            )
        }
    }

    private fun requestLocation() {
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

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        val locationManager = getSystemService(LocationManager::class.java)
        val provider =
            when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER

                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER

                else -> {
                    viewModel.reportLocationUnavailable()
                    return
                }
            }

        locationCancellationSignal?.cancel()
        locationCancellationSignal = CancellationSignal()
        LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            locationCancellationSignal,
            ContextCompat.getMainExecutor(this),
        ) { location ->
            if (location == null) {
                viewModel.reportLocationUnavailable()
            } else {
                viewModel.useDeviceLocation(
                    GeoPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                    ),
                )
            }
        }
    }

    override fun onDestroy() {
        locationCancellationSignal?.cancel()
        super.onDestroy()
    }
}


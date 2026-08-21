package com.zyb.deliciousfoodsearch.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.zyb.deliciousfoodsearch.domain.GeoPoint

data class DeviceLocationResult(
    val point: GeoPoint,
    val accuracyMeters: Float,
)

/**
 * 使用系统 GPS 与网络定位并行获取一次位置。
 *
 * GPS 在室内可能长时间没有结果，因此不能仅因 GPS 已开启就忽略网络定位。
 */
class SystemSingleLocationProvider(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeRequest: ActiveRequest? = null
    private var nextRequestId: Long = 0L

    @SuppressLint("MissingPermission")
    fun requestLocation(
        onSuccess: (DeviceLocationResult) -> Unit,
        onFailure: (Int) -> Unit,
    ) {
        cancel()
        val requestId = ++nextRequestId
        try {
            val locationManager = appContext.getSystemService(LocationManager::class.java)
            val providers =
                listOf(
                    LocationManager.NETWORK_PROVIDER,
                    LocationManager.GPS_PROVIDER,
                ).filter { provider -> locationManager.isProviderEnabled(provider) }

            if (providers.isEmpty()) {
                onFailure(ERROR_PROVIDER_DISABLED)
                return
            }

            val bestLastKnown =
                providers
                    .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                    .mapNotNull { location -> location.toCandidate(fromCurrentRequest = false) }
                    .filter(LocationFixSelector::isUsableFallback)
                    .reduceOrNull(LocationFixSelector::chooseBetter)

            val request =
                ActiveRequest(
                    id = requestId,
                    remainingProviders = providers.size,
                    bestCandidate = bestLastKnown,
                    onSuccess = onSuccess,
                    onFailure = onFailure,
                )
            activeRequest = request

            request.timeoutRunnable =
                Runnable {
                    finishWithBestCandidateOrFailure(request)
                }.also { timeout ->
                    mainHandler.postDelayed(timeout, LOCATION_TIMEOUT_MILLIS)
                }

            providers.forEach { provider ->
                if (activeRequest === request) {
                    requestProvider(locationManager, provider, request)
                }
            }
        } catch (_: SecurityException) {
            finishFailure(requestId, ERROR_PERMISSION_REVOKED, onFailure)
        } catch (_: RuntimeException) {
            finishFailure(requestId, ERROR_LOCATION_REQUEST_FAILED, onFailure)
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestProvider(
        locationManager: LocationManager,
        provider: String,
        request: ActiveRequest,
    ) {
        try {
            val signal = CancellationSignal()
            request.cancellationSignals += signal
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                signal,
                ContextCompat.getMainExecutor(appContext),
            ) { location ->
                if (activeRequest !== request) return@getCurrentLocation
                request.remainingProviders--
                location
                    ?.toCandidate(fromCurrentRequest = true)
                    ?.takeIf(LocationFixSelector::isUsableFallback)
                    ?.let { candidate ->
                        request.bestCandidate =
                            request.bestCandidate?.let { current ->
                                LocationFixSelector.chooseBetter(current, candidate)
                            } ?: candidate
                    }

                val bestCandidate = request.bestCandidate
                when {
                    bestCandidate != null && LocationFixSelector.isImmediatelyUsable(bestCandidate) ->
                        finishSuccess(request, bestCandidate)

                    request.remainingProviders == 0 ->
                        finishWithBestCandidateOrFailure(request)
                }
            }
        } catch (_: SecurityException) {
            finishFailure(request.id, ERROR_PERMISSION_REVOKED, request.onFailure)
        } catch (_: RuntimeException) {
            request.remainingProviders--
            if (request.remainingProviders == 0) {
                finishWithBestCandidateOrFailure(request)
            }
        }
    }

    fun cancel() {
        nextRequestId++
        activeRequest?.let(::clearRequest)
        activeRequest = null
    }

    private fun finishWithBestCandidateOrFailure(request: ActiveRequest) {
        if (activeRequest !== request) return
        request.bestCandidate?.let { candidate ->
            finishSuccess(request, candidate)
        } ?: run {
            clearRequest(request)
            activeRequest = null
            request.onFailure(ERROR_LOCATION_UNAVAILABLE)
        }
    }

    private fun finishSuccess(
        request: ActiveRequest,
        candidate: LocationFixCandidate,
    ) {
        if (activeRequest !== request) return
        clearRequest(request)
        activeRequest = null
        request.onSuccess(
            DeviceLocationResult(
                point = Gcj02CoordinateConverter.fromWgs84(candidate.point),
                accuracyMeters = candidate.accuracyMeters,
            ),
        )
    }

    private fun finishFailure(
        requestId: Long,
        errorCode: Int,
        onFailure: (Int) -> Unit,
    ) {
        val request = activeRequest
        if (request != null && request.id == requestId) {
            clearRequest(request)
            activeRequest = null
        }
        if ((request == null && requestId == nextRequestId) || request?.id == requestId) {
            onFailure(errorCode)
        }
    }

    private fun clearRequest(request: ActiveRequest) {
        request.timeoutRunnable?.let(mainHandler::removeCallbacks)
        request.timeoutRunnable = null
        request.cancellationSignals.forEach(CancellationSignal::cancel)
        request.cancellationSignals.clear()
    }

    private fun Location.toCandidate(fromCurrentRequest: Boolean): LocationFixCandidate? {
        val point = GeoPoint(latitude = latitude, longitude = longitude)
        if (!LocationFixSelector.hasValidCoordinates(point) || !hasAccuracy()) return null
        val ageMillis =
            if (fromCurrentRequest && time <= 0L) {
                0L
            } else {
                (clock() - time).coerceAtLeast(0L)
            }
        return LocationFixCandidate(
            point = point,
            accuracyMeters = accuracy.coerceAtLeast(1f),
            ageMillis = ageMillis,
        )
    }

    private data class ActiveRequest(
        val id: Long,
        var remainingProviders: Int,
        var bestCandidate: LocationFixCandidate?,
        val onSuccess: (DeviceLocationResult) -> Unit,
        val onFailure: (Int) -> Unit,
        val cancellationSignals: MutableList<CancellationSignal> = mutableListOf(),
        var timeoutRunnable: Runnable? = null,
    )

    companion object {
        private const val LOCATION_TIMEOUT_MILLIS = 12_000L

        const val ERROR_PROVIDER_DISABLED = 1
        const val ERROR_LOCATION_UNAVAILABLE = 2
        const val ERROR_PERMISSION_REVOKED = 3
        const val ERROR_LOCATION_REQUEST_FAILED = 4
    }
}

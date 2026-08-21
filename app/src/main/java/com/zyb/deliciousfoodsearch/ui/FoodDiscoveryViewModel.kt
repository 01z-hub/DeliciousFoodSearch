package com.zyb.deliciousfoodsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zyb.deliciousfoodsearch.data.AmapFoodDataSource
import com.zyb.deliciousfoodsearch.data.AmapSearchException
import com.zyb.deliciousfoodsearch.data.FoodDiscoveryRepository
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.HomeSection
import com.zyb.deliciousfoodsearch.domain.Restaurant
import com.zyb.deliciousfoodsearch.domain.RestaurantSectionSelector
import com.zyb.deliciousfoodsearch.domain.RestaurantSorter
import com.zyb.deliciousfoodsearch.domain.SortMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FoodDiscoveryUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val historyRestaurants: List<Restaurant> = emptyList(),
    val homeSection: HomeSection = HomeSection.AMAP_RECOMMENDED,
    val sortMode: SortMode? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val sourceLabel: String = AmapFoodDataSource.SOURCE_LABEL,
    val lastUpdatedEpochMillis: Long? = null,
    val userLocation: GeoPoint? = null,
    val isUsingManualLocation: Boolean = false,
    val locationMessage: String = "请先授权定位，以搜索你附近的真实餐厅",
) {
    val visibleRestaurants: List<Restaurant>
        get() =
            RestaurantSectionSelector.select(
                restaurants = restaurants,
                historyRestaurants = historyRestaurants,
                section = homeSection,
                auxiliarySortMode = sortMode,
            )
}

class FoodDiscoveryViewModel(
    private val repository: FoodDiscoveryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodDiscoveryUiState())
    val uiState: StateFlow<FoodDiscoveryUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null
    private var refreshGeneration: Long = 0L

    fun refresh() {
        val location = _uiState.value.userLocation
        if (location == null) {
            _uiState.update {
                it.copy(errorMessage = "还没有可用位置，请先定位或手动使用北京市中心")
            }
            return
        }
        startRefresh(location)
    }

    private fun startRefresh(location: GeoPoint) {
        refreshJob?.cancel()
        val generation = ++refreshGeneration
        refreshJob =
            viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val snapshot = repository.refresh(location)
                if (generation == refreshGeneration) {
                    _uiState.update {
                        it.copy(
                            restaurants = snapshot.restaurants,
                            historyRestaurants = snapshot.historyRestaurants,
                            isRefreshing = false,
                            sourceLabel = snapshot.sourceLabel,
                            lastUpdatedEpochMillis = snapshot.updatedAtEpochMillis,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (generation == refreshGeneration) {
                    _uiState.update { state ->
                        state.copy(
                            isRefreshing = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    fun setHomeSection(section: HomeSection) {
        _uiState.update { it.copy(homeSection = section, sortMode = null) }
    }

    fun setSortMode(sortMode: SortMode?) {
        _uiState.update { it.copy(sortMode = sortMode) }
    }

    fun reportLocationStarted() {
        _uiState.update {
            it.copy(
                locationMessage = "正在同时尝试网络定位和 GPS，请稍候…",
                errorMessage = null,
            )
        }
    }

    fun useDeviceLocation(
        location: GeoPoint,
        accuracyMeters: Float = 0f,
    ) {
        val accuracyMessage =
            if (accuracyMeters > 200f) {
                "当前位置精度约 ${accuracyMeters.toInt()} 米；如位置偏差较大，请在系统设置中开启精确位置"
            } else {
                "已使用设备当前位置，正在更新附近餐厅"
            }
        _uiState.update {
            it.copy(
                userLocation = location,
                isUsingManualLocation = false,
                locationMessage = accuracyMessage,
                errorMessage = null,
            )
        }
        startRefresh(location)
    }

    fun useBeijingCenter() {
        _uiState.update {
            it.copy(
                userLocation = BEIJING_CENTER,
                isUsingManualLocation = true,
                locationMessage = "已手动选择北京市中心；当前不是设备实时位置",
                errorMessage = null,
            )
        }
        startRefresh(BEIJING_CENTER)
    }

    fun recordVisit(restaurant: Restaurant) {
        val location = _uiState.value.userLocation ?: return
        viewModelScope.launch {
            try {
                val history =
                    withContext(Dispatchers.IO) {
                        repository.recordVisit(restaurant, location)
                    }
                val updated = history.firstOrNull { it.id == restaurant.id }
                _uiState.update { state ->
                    state.copy(
                        restaurants =
                            state.restaurants.map {
                                if (it.id == restaurant.id && updated != null) {
                                    it.copy(
                                        visitCount = updated.visitCount,
                                        lastVisitedAtEpochMillis = updated.lastVisitedAtEpochMillis,
                                    )
                                } else {
                                    it
                                }
                            },
                        historyRestaurants = history,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "常吃记录保存失败，请稍后重试")
                }
            }
        }
    }

    fun reportLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                locationMessage = "定位权限未授予；可以重试或手动使用北京市中心",
                errorMessage = null,
            )
        }
    }

    fun reportLocationUnavailable(errorCode: Int? = null) {
        val message =
            when (errorCode) {
                1 -> "系统定位服务尚未开启，请打开手机定位后重试"
                2 -> "GPS 和网络定位暂时都没有可用结果，请到开阔处重试"
                3 -> "定位权限已关闭，请在系统应用设置中允许位置信息"
                4 -> "手机系统定位服务请求失败，请关闭再打开定位后重试"
                else -> "暂时无法获取设备位置，请检查系统定位后重试"
            }
        _uiState.update {
            it.copy(
                locationMessage = message,
            )
        }
    }

    fun reportAmapInitializationUnavailable() {
        _uiState.update {
            it.copy(
                errorMessage = "高德服务初始化失败，请重新打开应用后重试",
            )
        }
    }

    private fun Exception.toUserMessage(): String =
        when (this) {
            is AmapSearchException ->
                when (errorCode) {
                    1002, 1008, 1009 -> "高德 Key 鉴权失败，请检查包名、SHA1 和 Key 配置"
                    1004, 1005 -> "高德查询额度或频率受限，请稍后重试"
                    else -> "高德餐厅更新失败（错误码 $errorCode），请稍后重试"
                }

            else -> "更新失败，请检查网络或稍后重试"
        }

    class Factory(
        private val repository: FoodDiscoveryRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(FoodDiscoveryViewModel::class.java))
            return FoodDiscoveryViewModel(repository) as T
        }
    }

    companion object {
        private val BEIJING_CENTER = GeoPoint(latitude = 39.9042, longitude = 116.4074)
    }
}

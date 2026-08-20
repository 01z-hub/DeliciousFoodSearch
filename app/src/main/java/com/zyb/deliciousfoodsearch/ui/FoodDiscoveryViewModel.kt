package com.zyb.deliciousfoodsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zyb.deliciousfoodsearch.data.FoodDiscoveryRepository
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.Restaurant
import com.zyb.deliciousfoodsearch.domain.RestaurantSorter
import com.zyb.deliciousfoodsearch.domain.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodDiscoveryUiState(
    val restaurants: List<Restaurant> = emptyList(),
    val sortMode: SortMode = SortMode.DISTANCE,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val sourceLabel: String = "演示数据 · 非抖音/美团实时信息",
    val lastUpdatedEpochMillis: Long? = null,
    val userLocation: GeoPoint = DEFAULT_DEMO_LOCATION,
    val isUsingDemoLocation: Boolean = true,
    val locationMessage: String = "当前使用北京演示位置；授权后将按真实位置重新计算距离",
) {
    val sortedRestaurants: List<Restaurant>
        get() = RestaurantSorter.sort(restaurants, sortMode)

    companion object {
        val DEFAULT_DEMO_LOCATION = GeoPoint(latitude = 39.9042, longitude = 116.4074)
    }
}

class FoodDiscoveryViewModel(
    private val repository: FoodDiscoveryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FoodDiscoveryUiState())
    val uiState: StateFlow<FoodDiscoveryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching {
                repository.refresh(_uiState.value.userLocation)
            }.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(
                        restaurants = snapshot.restaurants,
                        isRefreshing = false,
                        sourceLabel = snapshot.sourceLabel,
                        lastUpdatedEpochMillis = snapshot.updatedAtEpochMillis,
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isRefreshing = false,
                        errorMessage = "更新失败，请检查网络或稍后重试",
                    )
                }
            }
        }
    }

    fun setSortMode(sortMode: SortMode) {
        _uiState.update { it.copy(sortMode = sortMode) }
    }

    fun useDeviceLocation(location: GeoPoint) {
        _uiState.update {
            it.copy(
                userLocation = location,
                isUsingDemoLocation = false,
                locationMessage = "已使用设备当前位置，距离将在更新后重新计算",
            )
        }
        refresh()
    }

    fun reportLocationPermissionDenied() {
        _uiState.update {
            it.copy(
                isUsingDemoLocation = true,
                locationMessage = "定位权限未授予，继续使用北京演示位置",
            )
        }
    }

    fun reportLocationUnavailable() {
        _uiState.update {
            it.copy(
                locationMessage = "暂时无法获取设备位置，请检查系统定位后重试",
            )
        }
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
}


package com.zyb.deliciousfoodsearch.data

import android.content.Context
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItemV2
import com.amap.api.services.core.ServiceSettings
import com.amap.api.services.poisearch.PoiResultV2
import com.amap.api.services.poisearch.PoiSearchV2
import com.zyb.deliciousfoodsearch.domain.DistanceCalculator
import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.Restaurant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** 使用高德官方 Android 搜索 SDK 获取真实餐饮 POI。 */
class AmapFoodDataSource(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val searchRadiusMeters: Int = 5_000,
) : FoodDiscoveryDataSource {
    private val appContext = context.applicationContext

    override suspend fun discoverNearby(userLocation: GeoPoint): FoodCatalogSnapshot =
        withContext(Dispatchers.IO) {
            val result = awaitNearbySearch(userLocation)
            val now = clock()
            val restaurants =
                result
                    ?.pois
                    .orEmpty()
                    .mapIndexedNotNull { index, poi ->
                        poi.toRestaurantOrNull(
                            userLocation = userLocation,
                            weightRank = index + 1,
                            updatedAtEpochMillis = now,
                        )
                    }
            FoodCatalogSnapshot(
                restaurants = restaurants,
                updatedAtEpochMillis = now,
                sourceLabel = SOURCE_LABEL,
            )
        }

    /** 回调只传递原始结果，POI 映射继续在上层 Dispatchers.IO 中执行。 */
    private suspend fun awaitNearbySearch(userLocation: GeoPoint): PoiResultV2? =
        suspendCancellableCoroutine { continuation ->
            try {
                ServiceSettings.getInstance().setProtocol(ServiceSettings.HTTPS)
                val center = LatLonPoint(userLocation.latitude, userLocation.longitude)
                val query =
                    PoiSearchV2.Query("", FOOD_SERVICE_TYPE_CODE).apply {
                        pageNum = 1
                        pageSize = PAGE_SIZE
                        isDistanceSort = false
                        showFields = PoiSearchV2.ShowFields(PoiSearchV2.ShowFields.BUSINESS)
                    }
                val poiSearch =
                    PoiSearchV2(appContext, query).apply {
                        // false 表示使用高德综合权重排序，而不是距离排序。
                        bound = PoiSearchV2.SearchBound(center, searchRadiusMeters, false)
                    }

                poiSearch.setOnPoiSearchListener(
                    object : PoiSearchV2.OnPoiSearchListener {
                        override fun onPoiSearched(
                            result: PoiResultV2?,
                            errorCode: Int,
                        ) {
                            if (!continuation.isActive) return
                            if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                                continuation.resume(result)
                            } else {
                                continuation.resumeWithException(AmapSearchException(errorCode))
                            }
                        }

                        override fun onPoiItemSearched(
                            item: PoiItemV2?,
                            errorCode: Int,
                        ) = Unit
                    },
                )
                poiSearch.searchPOIAsyn()
            } catch (error: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }

    private fun PoiItemV2.toRestaurantOrNull(
        userLocation: GeoPoint,
        weightRank: Int,
        updatedAtEpochMillis: Long,
    ): Restaurant? {
        val poiPoint = latLonPoint ?: return null
        val poiName = title?.trim().orEmpty()
        if (poiName.isEmpty()) return null

        val restaurantLocation = GeoPoint(poiPoint.latitude, poiPoint.longitude)
        val businessInfo = business
        val rating = businessInfo?.getmRating().toPositiveDoubleOrNull() ?: 0.0
        val averagePrice = businessInfo?.cost.toPositiveDoubleOrNull()?.roundToInt()
        val category =
            typeDes
                ?.split(';')
                ?.asReversed()
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?: "餐饮"

        return Restaurant(
            id = poiId?.takeIf(String::isNotBlank) ?: "$poiName-${poiPoint.latitude}-${poiPoint.longitude}",
            name = poiName,
            category = category,
            rating = rating,
            // 搜索 SDK 2.0 不提供评价数量，不使用虚构数字。
            reviewCount = 0,
            averagePriceYuan = averagePrice,
            location = restaurantLocation,
            distanceMeters = DistanceCalculator.metersBetween(userLocation, restaurantLocation),
            // 营业时间字段不等价于实时营业状态，因此不做推断。
            isOpen = null,
            dishes = emptyList(),
            deals = emptyList(),
            sourceLabel = SOURCE_LABEL,
            updatedAtEpochMillis = updatedAtEpochMillis,
            address = snippet?.trim().orEmpty(),
            businessHours = businessInfo?.opentimeToday?.trim()?.takeIf(String::isNotEmpty),
            amapWeightRank = weightRank,
        )
    }

    private fun String?.toPositiveDoubleOrNull(): Double? =
        this
            ?.trim()
            ?.toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    companion object {
        private const val FOOD_SERVICE_TYPE_CODE = "050000"
        private const val PAGE_SIZE = 25
        const val SOURCE_LABEL = "高德地图真实 POI · 综合权重排序（非评分）"
    }
}

class AmapSearchException(
    val errorCode: Int,
) : Exception("高德搜索失败（错误码 $errorCode）")

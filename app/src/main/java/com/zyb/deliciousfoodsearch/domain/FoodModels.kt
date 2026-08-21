package com.zyb.deliciousfoodsearch.domain

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

data class Dish(
    val id: String,
    val name: String,
    val priceYuan: Int,
)

data class GroupDeal(
    val id: String,
    val title: String,
    val priceYuan: Int,
    val originalPriceYuan: Int,
    val suitablePeople: IntRange,
)

data class Restaurant(
    val id: String,
    val name: String,
    val category: String,
    val rating: Double,
    val reviewCount: Int,
    val averagePriceYuan: Int?,
    val location: GeoPoint,
    val distanceMeters: Int,
    val isOpen: Boolean?,
    val dishes: List<Dish>,
    val deals: List<GroupDeal>,
    val sourceLabel: String,
    val updatedAtEpochMillis: Long,
    val address: String = "",
    val businessHours: String? = null,
    /** 高德综合权重结果中的名次，不是用户评分。 */
    val amapWeightRank: Int? = null,
    val visitCount: Int = 0,
    val lastVisitedAtEpochMillis: Long? = null,
)

data class FoodCatalogSnapshot(
    val restaurants: List<Restaurant>,
    val updatedAtEpochMillis: Long,
    val sourceLabel: String,
    val historyRestaurants: List<Restaurant> = emptyList(),
)

enum class HomeSection(val label: String) {
    AMAP_RECOMMENDED("高德推荐"),
    FREQUENTLY_VISITED("历史常吃"),
    BEST_VALUE("最实惠"),
}

enum class SortMode(val label: String) {
    PRICE("价格从低到高"),
    TASTE("味道从高到低"),
    DISTANCE("距离从低到高"),
}

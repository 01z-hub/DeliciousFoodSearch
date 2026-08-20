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
    val isOpen: Boolean,
    val dishes: List<Dish>,
    val deals: List<GroupDeal>,
    val sourceLabel: String,
    val updatedAtEpochMillis: Long,
)

data class FoodCatalogSnapshot(
    val restaurants: List<Restaurant>,
    val updatedAtEpochMillis: Long,
    val sourceLabel: String,
)

enum class SortMode(val label: String) {
    PRICE("价格从低到高"),
    TASTE("味道从高到低"),
    DISTANCE("距离从低到高"),
}


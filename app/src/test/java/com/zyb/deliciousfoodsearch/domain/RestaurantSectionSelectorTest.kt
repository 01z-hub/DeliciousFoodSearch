package com.zyb.deliciousfoodsearch.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RestaurantSectionSelectorTest {
    private val nearby =
        listOf(
            restaurant(id = "rank-2", price = 30, rank = 2),
            restaurant(id = "rank-1", price = 80, rank = 1),
            restaurant(id = "unknown-price", price = null, rank = 3),
        )

    @Test
    fun amapRecommended_usesAmapWeightRankByDefault() {
        val result =
            RestaurantSectionSelector.select(
                restaurants = nearby,
                historyRestaurants = emptyList(),
                section = HomeSection.AMAP_RECOMMENDED,
                auxiliarySortMode = null,
            )

        assertEquals(listOf("rank-1", "rank-2", "unknown-price"), result.map { it.id })
    }

    @Test
    fun frequentlyVisited_usesCountThenLastVisitedTime() {
        val history =
            listOf(
                restaurant("older", 50, 1).copy(visitCount = 2, lastVisitedAtEpochMillis = 10L),
                restaurant("frequent", 60, 2).copy(visitCount = 3, lastVisitedAtEpochMillis = 5L),
                restaurant("newer", 70, 3).copy(visitCount = 2, lastVisitedAtEpochMillis = 20L),
            )

        val result =
            RestaurantSectionSelector.select(
                restaurants = nearby,
                historyRestaurants = history,
                section = HomeSection.FREQUENTLY_VISITED,
                auxiliarySortMode = null,
            )

        assertEquals(listOf("frequent", "newer", "older"), result.map { it.id })
    }

    @Test
    fun bestValue_putsUnknownPriceLast() {
        val result =
            RestaurantSectionSelector.select(
                restaurants = nearby,
                historyRestaurants = emptyList(),
                section = HomeSection.BEST_VALUE,
                auxiliarySortMode = null,
            )

        assertEquals(listOf("rank-2", "rank-1", "unknown-price"), result.map { it.id })
    }

    @Test
    fun auxiliaryDistanceSort_overridesSectionDefault() {
        val result =
            RestaurantSectionSelector.select(
                restaurants = nearby,
                historyRestaurants = emptyList(),
                section = HomeSection.AMAP_RECOMMENDED,
                auxiliarySortMode = SortMode.DISTANCE,
            )

        assertEquals(listOf("rank-1", "rank-2", "unknown-price"), result.map { it.id })
    }

    private fun restaurant(
        id: String,
        price: Int?,
        rank: Int,
    ) = Restaurant(
        id = id,
        name = id,
        category = "测试",
        rating = 4.5,
        reviewCount = 0,
        averagePriceYuan = price,
        location = GeoPoint(0.0, 0.0),
        distanceMeters = rank * 100,
        isOpen = null,
        dishes = emptyList(),
        deals = emptyList(),
        sourceLabel = "测试",
        updatedAtEpochMillis = 0L,
        amapWeightRank = rank,
    )
}

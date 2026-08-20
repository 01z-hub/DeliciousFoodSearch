package com.zyb.deliciousfoodsearch.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RestaurantSorterTest {
    private val restaurants =
        listOf(
            restaurant(id = "far-cheap", price = 30, rating = 4.5, reviews = 100, distance = 900),
            restaurant(id = "near-expensive", price = 90, rating = 4.8, reviews = 200, distance = 100),
            restaurant(id = "unknown-price", price = null, rating = 4.8, reviews = 800, distance = 300),
        )

    @Test
    fun priceSort_putsUnknownPriceLast() {
        val result = RestaurantSorter.sort(restaurants, SortMode.PRICE)

        assertEquals(listOf("far-cheap", "near-expensive", "unknown-price"), result.map { it.id })
    }

    @Test
    fun tasteSort_usesReviewCountAsTieBreaker() {
        val result = RestaurantSorter.sort(restaurants, SortMode.TASTE)

        assertEquals(listOf("unknown-price", "near-expensive", "far-cheap"), result.map { it.id })
    }

    @Test
    fun distanceSort_ordersNearestFirst() {
        val result = RestaurantSorter.sort(restaurants, SortMode.DISTANCE)

        assertEquals(listOf("near-expensive", "unknown-price", "far-cheap"), result.map { it.id })
    }

    private fun restaurant(
        id: String,
        price: Int?,
        rating: Double,
        reviews: Int,
        distance: Int,
    ) = Restaurant(
        id = id,
        name = id,
        category = "测试",
        rating = rating,
        reviewCount = reviews,
        averagePriceYuan = price,
        location = GeoPoint(0.0, 0.0),
        distanceMeters = distance,
        isOpen = true,
        dishes = emptyList(),
        deals = emptyList(),
        sourceLabel = "测试数据",
        updatedAtEpochMillis = 0L,
    )
}


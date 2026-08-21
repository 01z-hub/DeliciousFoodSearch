package com.zyb.deliciousfoodsearch.data

import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint

class FoodDiscoveryRepository(
    private val dataSource: FoodDiscoveryDataSource,
    private val historyStore: RestaurantHistoryStore = EmptyRestaurantHistoryStore,
) {
    suspend fun refresh(userLocation: GeoPoint): FoodCatalogSnapshot {
        val snapshot = dataSource.discoverNearby(userLocation)
        val history = historyStore.load(userLocation)
        val visitCounts = history.associateBy { it.id }
        return snapshot.copy(
            restaurants =
                snapshot.restaurants.map { restaurant ->
                    val historyItem = visitCounts[restaurant.id]
                    restaurant.copy(
                        visitCount = historyItem?.visitCount ?: 0,
                        lastVisitedAtEpochMillis = historyItem?.lastVisitedAtEpochMillis,
                    )
                },
            historyRestaurants = history,
        )
    }

    fun recordVisit(
        restaurant: com.zyb.deliciousfoodsearch.domain.Restaurant,
        userLocation: GeoPoint,
    ): List<com.zyb.deliciousfoodsearch.domain.Restaurant> {
        historyStore.recordVisit(restaurant)
        return historyStore.load(userLocation)
    }
}

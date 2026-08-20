package com.zyb.deliciousfoodsearch.data

import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint

class FoodDiscoveryRepository(
    private val dataSource: FoodDiscoveryDataSource,
) {
    suspend fun refresh(userLocation: GeoPoint): FoodCatalogSnapshot =
        dataSource.discoverNearby(userLocation)
}


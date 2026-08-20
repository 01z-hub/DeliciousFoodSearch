package com.zyb.deliciousfoodsearch.data

import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint

interface FoodDiscoveryDataSource {
    suspend fun discoverNearby(userLocation: GeoPoint): FoodCatalogSnapshot
}


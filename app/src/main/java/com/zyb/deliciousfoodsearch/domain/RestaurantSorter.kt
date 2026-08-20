package com.zyb.deliciousfoodsearch.domain

object RestaurantSorter {
    fun sort(restaurants: List<Restaurant>, sortMode: SortMode): List<Restaurant> =
        when (sortMode) {
            SortMode.PRICE ->
                restaurants.sortedWith(
                    compareBy<Restaurant> { it.averagePriceYuan == null }
                        .thenBy { it.averagePriceYuan ?: Int.MAX_VALUE }
                        .thenBy { it.distanceMeters }
                        .thenBy { it.id },
                )

            SortMode.TASTE ->
                restaurants.sortedWith(
                    compareByDescending<Restaurant> { it.rating }
                        .thenByDescending { it.reviewCount }
                        .thenBy { it.distanceMeters }
                        .thenBy { it.id },
                )

            SortMode.DISTANCE ->
                restaurants.sortedWith(
                    compareBy<Restaurant> { it.distanceMeters }
                        .thenByDescending { it.rating }
                        .thenBy { it.id },
                )
        }
}


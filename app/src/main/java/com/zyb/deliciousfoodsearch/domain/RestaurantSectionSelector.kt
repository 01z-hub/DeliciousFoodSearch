package com.zyb.deliciousfoodsearch.domain

object RestaurantSectionSelector {
    fun select(
        restaurants: List<Restaurant>,
        historyRestaurants: List<Restaurant>,
        section: HomeSection,
        auxiliarySortMode: SortMode?,
    ): List<Restaurant> {
        val sectionDefault =
            when (section) {
                HomeSection.AMAP_RECOMMENDED ->
                    restaurants.sortedBy { it.amapWeightRank ?: Int.MAX_VALUE }

                HomeSection.FREQUENTLY_VISITED ->
                    historyRestaurants.sortedWith(
                        compareByDescending<Restaurant> { it.visitCount }
                            .thenByDescending { it.lastVisitedAtEpochMillis ?: 0L }
                            .thenBy { it.name },
                    )

                HomeSection.BEST_VALUE ->
                    RestaurantSorter.sort(restaurants, SortMode.PRICE)
            }
        return auxiliarySortMode?.let { RestaurantSorter.sort(sectionDefault, it) } ?: sectionDefault
    }
}

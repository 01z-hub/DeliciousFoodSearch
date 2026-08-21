package com.zyb.deliciousfoodsearch.ui

import com.zyb.deliciousfoodsearch.data.FoodDiscoveryDataSource
import com.zyb.deliciousfoodsearch.data.FoodDiscoveryRepository
import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.Restaurant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDiscoveryViewModelTest {
    @Test
    fun newerLocationRequestWinsWhenPreviousRequestFinishesLater() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val dataSource = ControllableFoodDataSource()
                val viewModel =
                    FoodDiscoveryViewModel(
                        FoodDiscoveryRepository(dataSource),
                    )
                val firstLocation = GeoPoint(39.90, 116.40)
                val secondLocation = GeoPoint(31.23, 121.47)

                viewModel.useDeviceLocation(firstLocation)
                runCurrent()
                val firstRequest = dataSource.requests.receive()

                viewModel.useDeviceLocation(secondLocation)
                runCurrent()
                val secondRequest = dataSource.requests.receive()

                firstRequest.result.complete(snapshot("旧位置餐厅", 1L))
                secondRequest.result.complete(snapshot("新位置餐厅", 2L))
                runCurrent()

                assertEquals(secondLocation, viewModel.uiState.value.userLocation)
                assertEquals(listOf("新位置餐厅"), viewModel.uiState.value.restaurants.map { it.name })
                assertEquals(2L, viewModel.uiState.value.lastUpdatedEpochMillis)
                assertFalse(viewModel.uiState.value.isRefreshing)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun snapshot(
        restaurantName: String,
        updatedAt: Long,
    ): FoodCatalogSnapshot =
        FoodCatalogSnapshot(
            restaurants =
                listOf(
                    Restaurant(
                        id = restaurantName,
                        name = restaurantName,
                        category = "餐饮",
                        rating = 4.5,
                        reviewCount = 0,
                        averagePriceYuan = 50,
                        location = GeoPoint(39.90, 116.40),
                        distanceMeters = 100,
                        isOpen = null,
                        dishes = emptyList(),
                        deals = emptyList(),
                        sourceLabel = "测试",
                        updatedAtEpochMillis = updatedAt,
                    ),
                ),
            updatedAtEpochMillis = updatedAt,
            sourceLabel = "测试",
        )

    private class ControllableFoodDataSource : FoodDiscoveryDataSource {
        val requests = Channel<Request>(Channel.UNLIMITED)

        override suspend fun discoverNearby(userLocation: GeoPoint): FoodCatalogSnapshot {
            val result = CompletableDeferred<FoodCatalogSnapshot>()
            requests.send(Request(userLocation, result))
            return result.await()
        }
    }

    private data class Request(
        val location: GeoPoint,
        val result: CompletableDeferred<FoodCatalogSnapshot>,
    )
}

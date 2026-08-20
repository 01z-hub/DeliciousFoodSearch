package com.zyb.deliciousfoodsearch.data

import com.zyb.deliciousfoodsearch.domain.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFoodDataSourceTest {
    @Test
    fun discoverNearby_marksAllRecordsAsDemoAndCalculatesDistance() =
        runTest {
            val dataSource =
                DemoFoodDataSource(
                    clock = { 123_456L },
                    simulatedDelayMillis = 0L,
                )

            val snapshot = dataSource.discoverNearby(GeoPoint(39.9042, 116.4074))

            assertEquals(6, snapshot.restaurants.size)
            assertEquals(123_456L, snapshot.updatedAtEpochMillis)
            assertTrue(snapshot.sourceLabel.contains("演示数据"))
            assertTrue(snapshot.restaurants.all { it.sourceLabel.contains("演示数据") })
            assertTrue(snapshot.restaurants.all { it.distanceMeters > 0 })
            assertTrue(snapshot.restaurants.all { it.dishes.isNotEmpty() && it.deals.isNotEmpty() })
        }

    @Test
    fun repeatedRefresh_changesDemoPriceToProveRefreshFlow() =
        runTest {
            val dataSource = DemoFoodDataSource(simulatedDelayMillis = 0L)
            val location = GeoPoint(39.9042, 116.4074)

            val first = dataSource.discoverNearby(location)
            val second = dataSource.discoverNearby(location)
            val firstDealPrice =
                first.restaurants.first { it.id == "demo-noodles" }.deals.single().priceYuan
            val secondDealPrice =
                second.restaurants.first { it.id == "demo-noodles" }.deals.single().priceYuan

            assertNotEquals(firstDealPrice, secondDealPrice)
        }
}


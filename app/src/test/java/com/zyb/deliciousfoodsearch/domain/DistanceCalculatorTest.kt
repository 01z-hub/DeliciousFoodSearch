package com.zyb.deliciousfoodsearch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {
    @Test
    fun samePoint_hasZeroDistance() {
        val point = GeoPoint(latitude = 39.9042, longitude = 116.4074)

        assertEquals(0, DistanceCalculator.metersBetween(point, point))
    }

    @Test
    fun nearbyPoint_returnsExpectedApproximateDistance() {
        val start = GeoPoint(latitude = 39.9042, longitude = 116.4074)
        val end = GeoPoint(latitude = 39.9139, longitude = 116.4039)

        val distance = DistanceCalculator.metersBetween(start, end)

        assertTrue(distance in 1_000..1_200)
    }
}


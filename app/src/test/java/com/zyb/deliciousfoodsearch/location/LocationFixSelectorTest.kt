package com.zyb.deliciousfoodsearch.location

import com.zyb.deliciousfoodsearch.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFixSelectorTest {
    @Test
    fun preciseFreshLocationCanFinishWithoutWaitingForOtherProvider() {
        val candidate = candidate(accuracyMeters = 35f, ageMillis = 2_000L)

        assertTrue(LocationFixSelector.isImmediatelyUsable(candidate))
    }

    @Test
    fun staleLastKnownLocationIsNotUsedAsFallback() {
        val candidate = candidate(accuracyMeters = 20f, ageMillis = 11 * 60_000L)

        assertFalse(LocationFixSelector.isUsableFallback(candidate))
    }

    @Test
    fun recentNetworkLocationBeatsOldGpsLocationWhenGpsIsTooStale() {
        val recentNetwork = candidate(accuracyMeters = 350f, ageMillis = 2_000L)
        val oldGps = candidate(accuracyMeters = 20f, ageMillis = 5 * 60_000L)

        assertEquals(
            recentNetwork,
            LocationFixSelector.chooseBetter(recentNetwork, oldGps),
        )
    }

    @Test
    fun invalidCoordinatesAreRejected() {
        val candidate =
            LocationFixCandidate(
                point = GeoPoint(latitude = 91.0, longitude = 116.0),
                accuracyMeters = 10f,
                ageMillis = 0L,
            )

        assertFalse(LocationFixSelector.isUsableFallback(candidate))
    }

    private fun candidate(
        accuracyMeters: Float,
        ageMillis: Long,
    ) =
        LocationFixCandidate(
            point = GeoPoint(latitude = 39.9, longitude = 116.4),
            accuracyMeters = accuracyMeters,
            ageMillis = ageMillis,
        )
}

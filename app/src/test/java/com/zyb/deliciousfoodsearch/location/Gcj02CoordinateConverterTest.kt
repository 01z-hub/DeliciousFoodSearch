package com.zyb.deliciousfoodsearch.location

import com.zyb.deliciousfoodsearch.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Gcj02CoordinateConverterTest {
    @Test
    fun beijingWgs84_isConvertedToExpectedGcj02Range() {
        val result = Gcj02CoordinateConverter.fromWgs84(GeoPoint(39.9042, 116.4074))

        assertTrue(result.latitude in 39.9055..39.9057)
        assertTrue(result.longitude in 116.4135..116.4137)
    }

    @Test
    fun coordinateOutsideMainlandChina_isNotChanged() {
        val paris = GeoPoint(48.8566, 2.3522)

        assertEquals(paris, Gcj02CoordinateConverter.fromWgs84(paris))
    }
}

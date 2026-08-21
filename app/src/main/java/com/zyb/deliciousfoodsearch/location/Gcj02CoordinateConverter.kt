package com.zyb.deliciousfoodsearch.location

import com.zyb.deliciousfoodsearch.domain.GeoPoint
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/** 将系统 WGS84 坐标转换为高德在中国大陆使用的 GCJ-02 坐标。 */
object Gcj02CoordinateConverter {
    fun fromWgs84(point: GeoPoint): GeoPoint {
        if (isOutsideMainlandChina(point)) return point

        var latitudeDelta = transformLatitude(point.longitude - 105.0, point.latitude - 35.0)
        var longitudeDelta = transformLongitude(point.longitude - 105.0, point.latitude - 35.0)
        val radianLatitude = point.latitude / 180.0 * PI
        var magic = sin(radianLatitude)
        magic = 1 - EARTH_ECCENTRICITY * magic * magic
        val squareRootMagic = sqrt(magic)
        latitudeDelta =
            latitudeDelta * 180.0 /
                ((EARTH_RADIUS * (1 - EARTH_ECCENTRICITY) / (magic * squareRootMagic)) * PI)
        longitudeDelta =
            longitudeDelta * 180.0 /
                (EARTH_RADIUS / squareRootMagic * kotlin.math.cos(radianLatitude) * PI)
        return GeoPoint(
            latitude = point.latitude + latitudeDelta,
            longitude = point.longitude + longitudeDelta,
        )
    }

    private fun isOutsideMainlandChina(point: GeoPoint): Boolean =
        point.longitude !in 72.004..137.8347 || point.latitude !in 0.8293..55.8271

    private fun transformLatitude(
        x: Double,
        y: Double,
    ): Double {
        var result =
            -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y +
                0.1 * x * y + 0.2 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        result += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return result
    }

    private fun transformLongitude(
        x: Double,
        y: Double,
    ): Double {
        var result =
            300.0 + x + 2.0 * y + 0.1 * x * x +
                0.1 * x * y + 0.1 * sqrt(kotlin.math.abs(x))
        result += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        result += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        result += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return result
    }

    private const val EARTH_RADIUS = 6_378_245.0
    private const val EARTH_ECCENTRICITY = 0.00669342162296594323
}

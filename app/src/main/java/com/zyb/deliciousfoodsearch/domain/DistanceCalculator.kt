package com.zyb.deliciousfoodsearch.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceCalculator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun metersBetween(start: GeoPoint, end: GeoPoint): Int {
        val latitudeDelta = Math.toRadians(end.latitude - start.latitude)
        val longitudeDelta = Math.toRadians(end.longitude - start.longitude)
        val startLatitude = Math.toRadians(start.latitude)
        val endLatitude = Math.toRadians(end.latitude)

        val haversine =
            sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
                cos(startLatitude) * cos(endLatitude) *
                sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val centralAngle = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

        return (EARTH_RADIUS_METERS * centralAngle).roundToInt()
    }
}


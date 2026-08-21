package com.zyb.deliciousfoodsearch.location

import com.zyb.deliciousfoodsearch.domain.GeoPoint

internal data class LocationFixCandidate(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val ageMillis: Long,
)

/** 纯逻辑的位置质量规则，避免用陈旧但精度数字较小的位置覆盖当前网络定位。 */
internal object LocationFixSelector {
    private const val IMMEDIATE_MAX_ACCURACY_METERS = 200f
    private const val IMMEDIATE_MAX_AGE_MILLIS = 30_000L
    private const val FALLBACK_MAX_ACCURACY_METERS = 10_000f
    private const val FALLBACK_MAX_AGE_MILLIS = 10 * 60_000L
    private const val AGE_PENALTY_METERS_PER_SECOND = 2f

    fun hasValidCoordinates(point: GeoPoint): Boolean =
        point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0

    fun isImmediatelyUsable(candidate: LocationFixCandidate): Boolean =
        candidate.ageMillis <= IMMEDIATE_MAX_AGE_MILLIS &&
            candidate.accuracyMeters <= IMMEDIATE_MAX_ACCURACY_METERS

    fun isUsableFallback(candidate: LocationFixCandidate): Boolean =
        hasValidCoordinates(candidate.point) &&
            candidate.ageMillis <= FALLBACK_MAX_AGE_MILLIS &&
            candidate.accuracyMeters in 1f..FALLBACK_MAX_ACCURACY_METERS

    fun chooseBetter(
        first: LocationFixCandidate,
        second: LocationFixCandidate,
    ): LocationFixCandidate =
        if (qualityScore(first) <= qualityScore(second)) first else second

    private fun qualityScore(candidate: LocationFixCandidate): Float =
        candidate.accuracyMeters +
            (candidate.ageMillis / 1_000f) * AGE_PENALTY_METERS_PER_SECOND
}

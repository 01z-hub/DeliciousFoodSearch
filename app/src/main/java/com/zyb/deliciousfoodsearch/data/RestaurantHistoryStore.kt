package com.zyb.deliciousfoodsearch.data

import android.content.Context
import com.zyb.deliciousfoodsearch.domain.DistanceCalculator
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.Restaurant
import org.json.JSONArray
import org.json.JSONObject

interface RestaurantHistoryStore {
    fun load(userLocation: GeoPoint): List<Restaurant>

    fun recordVisit(restaurant: Restaurant): Restaurant
}

object EmptyRestaurantHistoryStore : RestaurantHistoryStore {
    override fun load(userLocation: GeoPoint): List<Restaurant> = emptyList()

    override fun recordVisit(restaurant: Restaurant): Restaurant = restaurant
}

/** 历史常吃仅保存在本机，不上传用户偏好或位置。 */
class SharedPreferencesRestaurantHistoryStore(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : RestaurantHistoryStore {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun load(userLocation: GeoPoint): List<Restaurant> =
        readRestaurants()
            .map { restaurant ->
                restaurant.copy(
                    distanceMeters =
                        DistanceCalculator.metersBetween(
                            userLocation,
                            restaurant.location,
                        ),
                )
            }.sortedWith(
                compareByDescending<Restaurant> { it.visitCount }
                    .thenByDescending { it.lastVisitedAtEpochMillis ?: 0L }
                    .thenBy { it.name },
            )

    @Synchronized
    override fun recordVisit(restaurant: Restaurant): Restaurant {
        val restaurants = readRestaurants().associateByTo(linkedMapOf()) { it.id }
        val previous = restaurants[restaurant.id]
        val updated =
            restaurant.copy(
                visitCount = (previous?.visitCount ?: restaurant.visitCount) + 1,
                lastVisitedAtEpochMillis = clock(),
            )
        restaurants[restaurant.id] = updated
        writeRestaurants(restaurants.values.toList())
        return updated
    }

    private fun readRestaurants(): List<Restaurant> {
        val raw = preferences.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getJSONObject(index).toRestaurant())
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeRestaurants(restaurants: List<Restaurant>) {
        val array = JSONArray()
        restaurants
            .sortedByDescending { it.lastVisitedAtEpochMillis ?: 0L }
            .take(MAX_HISTORY_SIZE)
            .forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    private fun Restaurant.toJson(): JSONObject =
        JSONObject().apply {
            put("id", id)
            put("name", name)
            put("category", category)
            put("rating", rating)
            put("reviewCount", reviewCount)
            put("averagePriceYuan", averagePriceYuan ?: JSONObject.NULL)
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("isOpen", isOpen ?: JSONObject.NULL)
            put("sourceLabel", sourceLabel)
            put("updatedAtEpochMillis", updatedAtEpochMillis)
            put("address", address)
            put("businessHours", businessHours ?: JSONObject.NULL)
            put("amapWeightRank", amapWeightRank ?: JSONObject.NULL)
            put("visitCount", visitCount)
            put("lastVisitedAtEpochMillis", lastVisitedAtEpochMillis ?: JSONObject.NULL)
        }

    private fun JSONObject.toRestaurant(): Restaurant =
        Restaurant(
            id = getString("id"),
            name = getString("name"),
            category = optString("category", "餐饮"),
            rating = optDouble("rating", 0.0),
            reviewCount = optInt("reviewCount", 0),
            averagePriceYuan = nullableInt("averagePriceYuan"),
            location = GeoPoint(getDouble("latitude"), getDouble("longitude")),
            distanceMeters = 0,
            isOpen = nullableBoolean("isOpen"),
            dishes = emptyList(),
            deals = emptyList(),
            sourceLabel = optString("sourceLabel", AmapFoodDataSource.SOURCE_LABEL),
            updatedAtEpochMillis = optLong("updatedAtEpochMillis", 0L),
            address = optString("address", ""),
            businessHours = nullableString("businessHours"),
            amapWeightRank = nullableInt("amapWeightRank"),
            visitCount = optInt("visitCount", 0),
            lastVisitedAtEpochMillis = nullableLong("lastVisitedAtEpochMillis"),
        )

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf(String::isNotBlank)

    private fun JSONObject.nullableInt(key: String): Int? =
        if (isNull(key) || !has(key)) null else getInt(key)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key) || !has(key)) null else getLong(key)

    private fun JSONObject.nullableBoolean(key: String): Boolean? =
        if (isNull(key) || !has(key)) null else getBoolean(key)

    companion object {
        private const val PREFERENCES_NAME = "restaurant_history"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY_SIZE = 50
    }
}

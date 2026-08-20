package com.zyb.deliciousfoodsearch.data

import com.zyb.deliciousfoodsearch.domain.Dish
import com.zyb.deliciousfoodsearch.domain.DistanceCalculator
import com.zyb.deliciousfoodsearch.domain.FoodCatalogSnapshot
import com.zyb.deliciousfoodsearch.domain.GeoPoint
import com.zyb.deliciousfoodsearch.domain.GroupDeal
import com.zyb.deliciousfoodsearch.domain.Restaurant
import kotlinx.coroutines.delay

/**
 * 明确标注的演示数据源。
 *
 * 抖音生活服务商品查询需要平台权限和商家授权，因此客户端不得持有平台密钥，
 * 也不得通过抓取页面伪装成真实团购数据。拿到合规服务端接口后替换此实现即可。
 */
class DemoFoodDataSource(
    private val clock: () -> Long = System::currentTimeMillis,
    private val simulatedDelayMillis: Long = 650L,
) : FoodDiscoveryDataSource {
    private var revision = 0

    override suspend fun discoverNearby(userLocation: GeoPoint): FoodCatalogSnapshot {
        delay(simulatedDelayMillis)
        revision += 1
        val now = clock()
        val source = "演示数据 · 非抖音/美团实时信息"

        val restaurantSeeds =
            listOf(
                RestaurantSeed(
                    id = "demo-hotpot",
                    name = "周末小火锅",
                    category = "火锅",
                    rating = 4.8,
                    reviewCount = 1286,
                    averagePrice = 79 + revision % 2,
                    latitudeOffset = 0.0031,
                    longitudeOffset = 0.0017,
                    dishes = listOf("招牌番茄锅" to 38, "鲜切牛肉" to 42, "蔬菜拼盘" to 18),
                    deals = listOf(DealSeed("双人暖心套餐", 138, 196, 2..2)),
                ),
                RestaurantSeed(
                    id = "demo-noodles",
                    name = "巷口手作面",
                    category = "面食",
                    rating = 4.6,
                    reviewCount = 832,
                    averagePrice = 29,
                    latitudeOffset = -0.0018,
                    longitudeOffset = 0.0022,
                    dishes = listOf("招牌牛肉面" to 28, "油泼面" to 24, "凉拌小菜" to 9),
                    deals = listOf(DealSeed("双人面食组合", 52 - revision % 2, 66, 2..2)),
                ),
                RestaurantSeed(
                    id = "demo-bbq",
                    name = "晚风炭火烤肉",
                    category = "烤肉",
                    rating = 4.9,
                    reviewCount = 2190,
                    averagePrice = 108,
                    latitudeOffset = 0.0062,
                    longitudeOffset = -0.0038,
                    dishes = listOf("厚切牛小排" to 68, "秘制五花肉" to 42, "烤口蘑" to 16),
                    deals = listOf(DealSeed("2–3 人精选套餐", 228 + revision % 3, 326, 2..3)),
                ),
                RestaurantSeed(
                    id = "demo-bistro",
                    name = "两个人小馆",
                    category = "融合菜",
                    rating = 4.7,
                    reviewCount = 613,
                    averagePrice = 92,
                    latitudeOffset = -0.0044,
                    longitudeOffset = -0.0029,
                    dishes = listOf("黑椒牛肉粒" to 58, "芝士焗南瓜" to 32, "海鲜烩饭" to 48),
                    deals = listOf(DealSeed("约会双人餐", 168, 238, 2..2)),
                ),
                RestaurantSeed(
                    id = "demo-dim-sum",
                    name = "早茶研究所",
                    category = "粤式点心",
                    rating = 4.5,
                    reviewCount = 1445,
                    averagePrice = 61,
                    latitudeOffset = 0.0025,
                    longitudeOffset = -0.0061,
                    dishes = listOf("虾饺皇" to 32, "流沙包" to 22, "豉汁凤爪" to 29),
                    deals = listOf(DealSeed("3–4 人点心套餐", 198 - revision % 3, 286, 3..4)),
                ),
                RestaurantSeed(
                    id = "demo-family",
                    name = "围桌家常菜",
                    category = "家常菜",
                    rating = 4.4,
                    reviewCount = 478,
                    averagePrice = null,
                    latitudeOffset = -0.0071,
                    longitudeOffset = 0.0045,
                    dishes = listOf("糖醋里脊" to 46, "宫保鸡丁" to 38, "干锅花菜" to 32),
                    deals = listOf(DealSeed("5–6 人欢聚餐", 368 + revision % 4, 498, 5..6)),
                ),
            )

        val restaurants =
            restaurantSeeds.map { seed ->
                val restaurantLocation =
                    GeoPoint(
                        latitude = userLocation.latitude + seed.latitudeOffset,
                        longitude = userLocation.longitude + seed.longitudeOffset,
                    )
                Restaurant(
                    id = seed.id,
                    name = seed.name,
                    category = seed.category,
                    rating = seed.rating,
                    reviewCount = seed.reviewCount,
                    averagePriceYuan = seed.averagePrice,
                    location = restaurantLocation,
                    distanceMeters = DistanceCalculator.metersBetween(userLocation, restaurantLocation),
                    isOpen = seed.id != "demo-family" || revision % 3 != 0,
                    dishes =
                        seed.dishes.mapIndexed { index, (name, price) ->
                            Dish(
                                id = "${seed.id}-dish-$index",
                                name = name,
                                priceYuan = price + if (index == 0) revision % 2 else 0,
                            )
                        },
                    deals =
                        seed.deals.mapIndexed { index, deal ->
                            GroupDeal(
                                id = "${seed.id}-deal-$index",
                                title = deal.title,
                                priceYuan = deal.price,
                                originalPriceYuan = deal.originalPrice,
                                suitablePeople = deal.people,
                            )
                        },
                    sourceLabel = source,
                    updatedAtEpochMillis = now,
                )
            }

        return FoodCatalogSnapshot(
            restaurants = restaurants,
            updatedAtEpochMillis = now,
            sourceLabel = source,
        )
    }

    private data class RestaurantSeed(
        val id: String,
        val name: String,
        val category: String,
        val rating: Double,
        val reviewCount: Int,
        val averagePrice: Int?,
        val latitudeOffset: Double,
        val longitudeOffset: Double,
        val dishes: List<Pair<String, Int>>,
        val deals: List<DealSeed>,
    )

    private data class DealSeed(
        val title: String,
        val price: Int,
        val originalPrice: Int,
        val people: IntRange,
    )
}


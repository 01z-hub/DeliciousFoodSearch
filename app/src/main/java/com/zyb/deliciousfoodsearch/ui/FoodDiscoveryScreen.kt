package com.zyb.deliciousfoodsearch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zyb.deliciousfoodsearch.domain.GroupDeal
import com.zyb.deliciousfoodsearch.domain.Restaurant
import com.zyb.deliciousfoodsearch.domain.SortMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FoodOrange = Color(0xFFE85D2A)
private val FoodCream = Color(0xFFFFF8F3)
private val DemoYellow = Color(0xFFFFF1C2)

@Composable
fun DeliciousFoodSearchApp(
    viewModel: FoodDiscoveryViewModel,
    onRequestLocation: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = FoodOrange,
                background = FoodCream,
                surface = Color.White,
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            FoodDiscoveryScreen(
                state = state,
                onRefresh = viewModel::refresh,
                onSortModeSelected = viewModel::setSortMode,
                onRequestLocation = onRequestLocation,
            )
        }
    }
}

@Composable
private fun FoodDiscoveryScreen(
    state: FoodDiscoveryUiState,
    onRefresh: () -> Unit,
    onSortModeSelected: (SortMode) -> Unit,
    onRequestLocation: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(FoodCream),
    ) {
        Header(
            state = state,
            onRefresh = onRefresh,
            onRequestLocation = onRequestLocation,
        )
        SortModeRow(
            selectedMode = state.sortMode,
            onSortModeSelected = onSortModeSelected,
        )

        when {
            state.isRefreshing && state.restaurants.isEmpty() ->
                LoadingState()

            state.errorMessage != null && state.restaurants.isEmpty() ->
                ErrorState(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                )

            state.sortedRestaurants.isEmpty() ->
                EmptyState(onRetry = onRefresh)

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = state.sortedRestaurants,
                        key = { it.id },
                    ) { restaurant ->
                        RestaurantCard(restaurant)
                    }
                }
        }
    }
}

@Composable
private fun Header(
    state: FoodDiscoveryUiState,
    onRefresh: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 12.dp),
    ) {
        Text(
            text = "周末吃什么",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "附近好店和团购，一次看清",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(DemoYellow, RoundedCornerShape(12.dp))
                    .padding(12.dp),
        ) {
            Text(
                text = state.sourceLabel,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "当前数据仅用于验证产品流程，不代表真实商家或平台价格。",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = state.locationMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRequestLocation) {
                Text(if (state.isUsingDemoLocation) "使用当前位置" else "重新定位")
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onRefresh,
                enabled = !state.isRefreshing,
            ) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("更新中")
                } else {
                    Text("更新商家与价格")
                }
            }
        }

        state.lastUpdatedEpochMillis?.let {
            Text(
                text = "最后更新：${formatUpdatedTime(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.errorMessage?.let {
            Text(
                text = "$it，当前仍展示上次成功数据。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SortModeRow(
    selectedMode: SortMode,
    onSortModeSelected: (SortMode) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortMode.entries.forEach { mode ->
            if (mode == selectedMode) {
                Button(onClick = { onSortModeSelected(mode) }) {
                    Text(mode.label)
                }
            } else {
                OutlinedButton(onClick = { onSortModeSelected(mode) }) {
                    Text(mode.label)
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFFEADFD7))
}

@Composable
private fun RestaurantCard(restaurant: Restaurant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${restaurant.category} · ${if (restaurant.isOpen) "营业中" else "休息中"}",
                        color =
                            if (restaurant.isOpen) {
                                Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }
                Text(
                    text = formatDistance(restaurant.distanceMeters),
                    fontWeight = FontWeight.SemiBold,
                    color = FoodOrange,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    "味道 ${restaurant.rating}（${restaurant.reviewCount} 条演示评价） · " +
                        "人均 ${restaurant.averagePriceYuan?.let { "¥$it" } ?: "未知"}",
            )

            Spacer(Modifier.height(12.dp))
            Text("菜品", fontWeight = FontWeight.SemiBold)
            restaurant.dishes.forEach { dish ->
                Text(
                    text = "• ${dish.name}  ¥${dish.priceYuan}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (restaurant.deals.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("团购", fontWeight = FontWeight.SemiBold)
                restaurant.deals.forEach { deal ->
                    DealRow(deal)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = restaurant.sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DealRow(deal: GroupDeal) {
    val people =
        if (deal.suitablePeople.first == deal.suitablePeople.last) {
            "${deal.suitablePeople.first} 人"
        } else {
            "${deal.suitablePeople.first}–${deal.suitablePeople.last} 人"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${deal.title}（适合 $people）",
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "¥${deal.priceYuan}",
            color = FoodOrange,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "原价 ¥${deal.originalPriceYuan}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("正在更新附近商家、菜品和团购…")
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("附近暂时没有找到可展示的美食")
            TextButton(onClick = onRetry) {
                Text("重新搜索")
            }
        }
    }
}

private fun formatDistance(distanceMeters: Int): String =
    if (distanceMeters < 1_000) {
        "${distanceMeters}m"
    } else {
        String.format(Locale.CHINA, "%.1fkm", distanceMeters / 1_000.0)
    }

private fun formatUpdatedTime(epochMillis: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA).format(Date(epochMillis))


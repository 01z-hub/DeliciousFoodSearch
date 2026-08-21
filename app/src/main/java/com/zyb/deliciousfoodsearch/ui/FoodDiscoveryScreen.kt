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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zyb.deliciousfoodsearch.domain.HomeSection
import com.zyb.deliciousfoodsearch.domain.Restaurant
import com.zyb.deliciousfoodsearch.domain.SortMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BerryPink = Color(0xFFC83E6B)
private val DeepBerry = Color(0xFF69243E)
private val PetalPink = Color(0xFFFFE5EE)
private val SoftBlush = Color(0xFFFFF5F8)
private val WarmWhite = Color(0xFFFFFBFC)
private val RoseBorder = Color(0xFFF3C7D5)
private val MutedRose = Color(0xFF8A6270)
private val SuccessGreen = Color(0xFF357A5B)

@Composable
fun DeliciousFoodSearchApp(
    viewModel: FoodDiscoveryViewModel,
    privacyAccepted: Boolean,
    amapReady: Boolean,
    showPrivacyDialog: Boolean,
    onRequestPrivacyConsent: () -> Unit,
    onAcceptPrivacy: () -> Unit,
    onDeclinePrivacy: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onRequestLocation: () -> Unit,
    onUseBeijingCenter: () -> Unit,
    onOpenMeituan: (String) -> Unit,
    onOpenDouyin: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = BerryPink,
                onPrimary = Color.White,
                primaryContainer = PetalPink,
                onPrimaryContainer = DeepBerry,
                secondary = Color(0xFF9B5570),
                background = SoftBlush,
                onBackground = DeepBerry,
                surface = WarmWhite,
                onSurface = DeepBerry,
                surfaceVariant = Color(0xFFFFEDF3),
                onSurfaceVariant = MutedRose,
                outline = RoseBorder,
                error = Color(0xFFB3264F),
            ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = SoftBlush) {
            FoodDiscoveryScreen(
                state = state,
                privacyAccepted = privacyAccepted,
                amapReady = amapReady,
                onRequestPrivacyConsent = onRequestPrivacyConsent,
                onRefresh = viewModel::refresh,
                onHomeSectionSelected = viewModel::setHomeSection,
                onSortModeSelected = viewModel::setSortMode,
                onRequestLocation = onRequestLocation,
                onUseBeijingCenter = onUseBeijingCenter,
                onRecordVisit = viewModel::recordVisit,
                onOpenMeituan = onOpenMeituan,
                onOpenDouyin = onOpenDouyin,
            )
        }

        if (showPrivacyDialog) {
            AmapPrivacyDialog(
                onAccept = onAcceptPrivacy,
                onDecline = onDeclinePrivacy,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            )
        }
    }
}

@Composable
private fun FoodDiscoveryScreen(
    state: FoodDiscoveryUiState,
    privacyAccepted: Boolean,
    amapReady: Boolean,
    onRequestPrivacyConsent: () -> Unit,
    onRefresh: () -> Unit,
    onHomeSectionSelected: (HomeSection) -> Unit,
    onSortModeSelected: (SortMode?) -> Unit,
    onRequestLocation: () -> Unit,
    onUseBeijingCenter: () -> Unit,
    onRecordVisit: (Restaurant) -> Unit,
    onOpenMeituan: (String) -> Unit,
    onOpenDouyin: (String) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(SoftBlush),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            WeekendHero()
        }
        item {
            LocationAndRefreshCard(
                state = state,
                privacyAccepted = privacyAccepted,
                amapReady = amapReady,
                onRequestPrivacyConsent = onRequestPrivacyConsent,
                onRefresh = onRefresh,
                onRequestLocation = onRequestLocation,
                onUseBeijingCenter = onUseBeijingCenter,
            )
        }
        item {
            SectionTitle(
                eyebrow = "为你们挑选",
                title = "今天从哪一栏开始？",
            )
        }
        item {
            HomeSectionRow(
                selectedSection = state.homeSection,
                onSectionSelected = onHomeSectionSelected,
            )
        }
        item {
            SortModeRow(
                selectedMode = state.sortMode,
                onSortModeSelected = onSortModeSelected,
            )
        }

        when {
            !privacyAccepted ->
                item {
                    StateCard(
                        emoji = "🌷",
                        title = "先开启附近美食",
                        message = "同意高德服务隐私说明后，才会使用一次系统定位查找周边餐厅。",
                        actionLabel = "查看并授权",
                        onAction = onRequestPrivacyConsent,
                    )
                }

            !amapReady ->
                item {
                    LoadingCard("正在准备高德餐厅服务…")
                }

            state.isRefreshing && state.restaurants.isEmpty() ->
                item {
                    LoadingCard("正在认真挑选附近餐厅…")
                }

            state.userLocation == null ->
                item {
                    LocationRequiredCard(
                        message = state.errorMessage ?: state.locationMessage,
                        onRequestLocation = onRequestLocation,
                        onUseBeijingCenter = onUseBeijingCenter,
                    )
                }

            state.errorMessage != null && state.restaurants.isEmpty() ->
                item {
                    StateCard(
                        emoji = "🥺",
                        title = "这次没有找到餐厅",
                        message = state.errorMessage,
                        actionLabel = "再试一次",
                        onAction = onRefresh,
                    )
                }

            state.visibleRestaurants.isEmpty() ->
                item {
                    EmptyStateCard(
                        section = state.homeSection,
                        onRetry = onRefresh,
                    )
                }

            else -> {
                item {
                    Text(
                        text = sectionExplanation(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedRose,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                items(
                    items = state.visibleRestaurants,
                    key = { it.id },
                ) { restaurant ->
                    RestaurantCard(
                        restaurant = restaurant,
                        showWeightRank =
                            state.homeSection == HomeSection.AMAP_RECOMMENDED &&
                                state.sortMode == null,
                        onRecordVisit = { onRecordVisit(restaurant) },
                        onOpenMeituan = { onOpenMeituan(restaurant.name) },
                        onOpenDouyin = { onOpenDouyin(restaurant.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekendHero() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    brush =
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD9E6), Color(0xFFFFEEF4)),
                        ),
                    shape = RoundedCornerShape(28.dp),
                ).padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Column {
            Surface(
                color = Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = "💕 两个人的周末",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepBerry,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "今天，想吃点什么？",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBerry,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = "把周末留给约会，不留给选择题 ✨",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedRose,
            )
        }
    }
}

@Composable
private fun LocationAndRefreshCard(
    state: FoodDiscoveryUiState,
    privacyAccepted: Boolean,
    amapReady: Boolean,
    onRequestPrivacyConsent: () -> Unit,
    onRefresh: () -> Unit,
    onRequestLocation: () -> Unit,
    onUseBeijingCenter: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .background(PetalPink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⌖", style = MaterialTheme.typography.titleLarge, color = BerryPink)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "你们附近",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedRose,
                    )
                    Text(
                        text = statusTitle(state, privacyAccepted, amapReady),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBerry,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = if (privacyAccepted) onRefresh else onRequestPrivacyConsent,
                    enabled =
                        !privacyAccepted ||
                            (amapReady && state.userLocation != null && !state.isRefreshing),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (privacyAccepted) "刷新" else "开启")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRequestLocation,
                    enabled = privacyAccepted && amapReady,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text(if (state.userLocation == null) "使用当前位置" else "重新定位")
                }
                OutlinedButton(
                    onClick = onUseBeijingCenter,
                    enabled = privacyAccepted && amapReady,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text("手动选北京")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = sourceDescription(privacyAccepted, amapReady),
                style = MaterialTheme.typography.bodySmall,
                color = MutedRose,
            )
            state.lastUpdatedEpochMillis?.let {
                Text(
                    text = "更新于 ${formatUpdatedTime(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedRose,
                )
            }
            if (state.errorMessage != null && state.restaurants.isNotEmpty()) {
                Text(
                    text = "${state.errorMessage}，暂时保留上次结果。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    eyebrow: String,
    title: String,
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelMedium,
            color = BerryPink,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = DeepBerry,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeSectionRow(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeSection.entries.forEach { section ->
            val selected = section == selectedSection
            Surface(
                onClick = { onSectionSelected(section) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) BerryPink else WarmWhite,
                contentColor = if (selected) Color.White else DeepBerry,
                tonalElevation = if (selected) 3.dp else 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = sectionEmoji(section), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SortModeRow(
    selectedMode: SortMode?,
    onSortModeSelected: (SortMode?) -> Unit,
) {
    Column {
        Text(
            text = "还可以这样排序",
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MutedRose,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SortMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selectedMode,
                    onClick = {
                        onSortModeSelected(if (mode == selectedMode) null else mode)
                    },
                    label = { Text(mode.label) },
                )
            }
        }
    }
}

@Composable
private fun RestaurantCard(
    restaurant: Restaurant,
    showWeightRank: Boolean,
    onRecordVisit: () -> Unit,
    onOpenMeituan: () -> Unit,
    onOpenDouyin: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(PetalPink, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (showWeightRank && restaurant.amapWeightRank != null) "${restaurant.amapWeightRank}" else "♡",
                        style = MaterialTheme.typography.titleMedium,
                        color = BerryPink,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = restaurant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepBerry,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = restaurant.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedRose,
                    )
                }
                Surface(
                    color = PetalPink,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = formatDistance(restaurant.distanceMeters),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BerryPink,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(
                    text = "★ ${restaurant.rating.takeIf { it > 0.0 }?.let(::formatRating) ?: "暂无评分"}",
                    containerColor = Color(0xFFFFF1D8),
                    contentColor = Color(0xFF8A5B00),
                )
                InfoPill(
                    text = "人均 ${restaurant.averagePriceYuan?.let { "¥$it" } ?: "未知"}",
                    containerColor = Color(0xFFFFE8EF),
                    contentColor = DeepBerry,
                )
            }

            if (showWeightRank && restaurant.amapWeightRank != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "高德综合推荐第 ${restaurant.amapWeightRank} 位 · 不是用户评分",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (restaurant.address.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "⌖ ${restaurant.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedRose,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            restaurant.businessHours?.let {
                Text(
                    text = "营业时间  $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedRose,
                )
            }

            Spacer(Modifier.height(14.dp))
            FilledTonalButton(
                onClick = onRecordVisit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PetalPink),
            ) {
                Text(
                    if (restaurant.visitCount > 0) {
                        "♡ 已经吃过 ${restaurant.visitCount} 次，再记一次"
                    } else {
                        "♡ 记进我们的常吃清单"
                    },
                    color = DeepBerry,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenMeituan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("美团看团购")
                }
                OutlinedButton(
                    onClick = onOpenDouyin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("抖音看团购")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "高德提供商家、评分和参考人均；菜品与团购以跳转平台为准。",
                style = MaterialTheme.typography.labelSmall,
                color = MutedRose,
            )
        }
    }
}

@Composable
private fun InfoPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(color = containerColor, shape = RoundedCornerShape(50)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AmapPrivacyDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Text("🌸", style = MaterialTheme.typography.headlineMedium) },
        title = { Text("开启附近美食", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "为了查找附近餐厅，应用会通过系统定位单次获取位置，并在你同意后初始化高德搜索服务。不会在后台持续定位，常吃记录也只保存在本机。",
                )
                TextButton(onClick = onOpenPrivacyPolicy) {
                    Text("查看高德开放平台隐私政策")
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, shape = RoundedCornerShape(16.dp)) {
                Text("同意，开始挑选")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("暂时不用")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = WarmWhite,
    )
}

@Composable
private fun LoadingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = BerryPink)
            Spacer(Modifier.height(14.dp))
            Text(message, color = DeepBerry)
        }
    }
}

@Composable
private fun LocationRequiredCard(
    message: String,
    onRequestLocation: () -> Unit,
    onUseBeijingCenter: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("📍", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text("还需要一个位置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedRose)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth()) {
                Text("重新定位")
            }
            TextButton(onClick = onUseBeijingCenter) {
                Text("先看看北京市中心")
            }
        }
    }
}

@Composable
private fun StateCard(
    emoji: String,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MutedRose)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    section: HomeSection,
    onRetry: () -> Unit,
) {
    val message =
        when (section) {
            HomeSection.FREQUENTLY_VISITED -> "还没有常吃记录，在喜欢的餐厅卡片上点一下爱心就好。"
            HomeSection.BEST_VALUE -> "附近餐厅暂时没有可比较的参考人均价格。"
            HomeSection.AMAP_RECOMMENDED -> "附近暂时没有找到可展示的餐厅。"
        }
    StateCard(
        emoji = "🍰",
        title = "这里还空空的",
        message = message,
        actionLabel = "重新搜索",
        onAction = onRetry,
    )
}

private fun statusTitle(
    state: FoodDiscoveryUiState,
    privacyAccepted: Boolean,
    amapReady: Boolean,
): String =
    when {
        !privacyAccepted -> "开启定位后，帮你们找附近好吃的"
        !amapReady -> "正在准备餐厅服务…"
        else -> state.locationMessage
    }

private fun sourceDescription(
    privacyAccepted: Boolean,
    amapReady: Boolean,
): String =
    when {
        !privacyAccepted -> "只会在同意后初始化高德服务，不会持续后台定位。"
        !amapReady -> "高德服务正在后台初始化，请稍等片刻。"
        else -> "真实餐厅数据来自高德；团购价格请跳转美团或抖音核实。"
    }

private fun sectionEmoji(section: HomeSection): String =
    when (section) {
        HomeSection.AMAP_RECOMMENDED -> "✨"
        HomeSection.FREQUENTLY_VISITED -> "💕"
        HomeSection.BEST_VALUE -> "🌷"
    }

private fun sectionExplanation(state: FoodDiscoveryUiState): String =
    state.sortMode?.let { "当前按“${it.label}”展示" }
        ?: when (state.homeSection) {
            HomeSection.AMAP_RECOMMENDED -> "保留高德返回的综合权重顺序；高德不公开具体 weight 数值。"
            HomeSection.FREQUENTLY_VISITED -> "按本机记录的吃过次数排序，次数相同则最近记录优先。"
            HomeSection.BEST_VALUE -> "按高德参考人均价格从低到高排列，价格未知的餐厅放在最后。"
        }

private fun formatDistance(distanceMeters: Int): String =
    if (distanceMeters < 1_000) {
        "${distanceMeters}m"
    } else {
        String.format(Locale.CHINA, "%.1fkm", distanceMeters / 1_000.0)
    }

private fun formatRating(rating: Double): String = String.format(Locale.CHINA, "%.1f", rating)

private fun formatUpdatedTime(epochMillis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(epochMillis))

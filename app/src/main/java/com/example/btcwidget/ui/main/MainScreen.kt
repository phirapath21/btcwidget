package com.example.btcwidget.ui.main

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.btcwidget.BtcWidgetProvider
import com.example.btcwidget.BtcDualWidgetProvider
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.btcwidget.PriceData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = remember { context.getSharedPreferences("blockclock_prefs", Context.MODE_PRIVATE) }
    var currentTheme by remember { mutableStateOf(prefs.getInt("blockclock_theme", 0)) }

    LaunchedEffect(Unit) {
        viewModel.loadInitialData(context)
    }

    val onThemeSelected: (Int) -> Unit = { newTheme ->
        prefs.edit().putInt("blockclock_theme", newTheme).apply()
        currentTheme = newTheme
        // Broadcast custom action to refresh widgets instantly
        val intent = Intent(context, BtcWidgetProvider::class.java).apply {
            action = BtcWidgetProvider.ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }

    // Map theme colors
    val themeBgColor: Color
    val themeCardBgColor: Color
    val themeValueColor: Color
    val themeLabelColor: Color
    val themeSubTextColor: Color
    val themeBorderColor: Color

    when (currentTheme) {
        1 -> { // E-Ink Light
            themeBgColor = Color(0xFFE4E4E7)
            themeCardBgColor = Color(0xFFF4F4F5)
            themeValueColor = Color(0xFF18181B)
            themeLabelColor = Color(0xFF71717A)
            themeSubTextColor = Color(0xFF71717A)
            themeBorderColor = Color(0xFF18181B)
        }
        2 -> { // Bitcoin Orange
            themeBgColor = Color(0xFF78350F)
            themeCardBgColor = Color(0xFFF7931A)
            themeValueColor = Color(0xFF121214)
            themeLabelColor = Color(0xFF8A6D3B)
            themeSubTextColor = Color(0xFF78350F)
            themeBorderColor = Color(0xFF121214)
        }
        3 -> { // Matrix Green
            themeBgColor = Color(0xFF022C22)
            themeCardBgColor = Color(0xFF0A0A0C)
            themeValueColor = Color(0xFF10B981)
            themeLabelColor = Color(0xFF047857)
            themeSubTextColor = Color(0xFF047857)
            themeBorderColor = Color(0xFF10B981)
        }
        4 -> { // Coinkite Gold
            themeBgColor = Color(0xFF0C0C0E)
            themeCardBgColor = Color(0xFF121214)
            themeValueColor = Color(0xFFC5A059)
            themeLabelColor = Color(0xFF8A6D3B)
            themeSubTextColor = Color(0xFF8A6D3B)
            themeBorderColor = Color(0xFFC5A059)
        }
        5 -> { // Amber Phosphor
            themeBgColor = Color(0xFF1C0F02)
            themeCardBgColor = Color(0xFF0A0A0C)
            themeValueColor = Color(0xFFF59E0B)
            themeLabelColor = Color(0xFFD97706)
            themeSubTextColor = Color(0xFFD97706)
            themeBorderColor = Color(0xFFF59E0B)
        }
        6 -> { // GameBoy Classic
            themeBgColor = Color(0xFF306230)
            themeCardBgColor = Color(0xFF8BAC0F)
            themeValueColor = Color(0xFF0F380F)
            themeLabelColor = Color(0xFF306230)
            themeSubTextColor = Color(0xFF306230)
            themeBorderColor = Color(0xFF0F380F)
        }
        else -> { // E-Ink Dark (Default)
            themeBgColor = Color(0xFF0C0C0E)
            themeCardBgColor = Color(0xFF121214)
            themeValueColor = Color(0xFFE4E4E7)
            themeLabelColor = Color(0xFF8E8E93)
            themeSubTextColor = Color(0xFF8E8E93)
            themeBorderColor = Color(0xFFC5A059)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBgColor)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(titleColor = themeBorderColor, subtitleColor = themeLabelColor)

            // Theme selector UI
            ThemeSelectorSection(currentTheme = currentTheme, onThemeSelected = onThemeSelected, labelColor = themeLabelColor)

            Spacer(modifier = Modifier.height(16.dp))

            when (val currentState = state) {
                is MainScreenUiState.Loading -> {
                    LoadingScreen()
                }
                is MainScreenUiState.Success -> {
                    DashboardContent(
                        data = currentState.data,
                        onRefresh = { viewModel.refreshData(context) },
                        isLoading = false,
                        cardBgColor = themeCardBgColor,
                        borderColor = themeBorderColor,
                        valueColor = themeValueColor,
                        labelColor = themeLabelColor,
                        subTextColor = themeSubTextColor,
                        context = context,
                        prefs = prefs,
                        onSettingsChanged = {
                            viewModel.refreshData(context)
                            val widgetIntent = Intent(context, BtcWidgetProvider::class.java).apply {
                                action = BtcWidgetProvider.ACTION_REFRESH
                            }
                            context.sendBroadcast(widgetIntent)
                            val dualWidgetIntent = Intent(context, BtcDualWidgetProvider::class.java).apply {
                                action = BtcDualWidgetProvider.ACTION_REFRESH
                            }
                            context.sendBroadcast(dualWidgetIntent)
                        }
                    )
                }
                is MainScreenUiState.Error -> {
                    ErrorScreen(
                        error = currentState.throwable,
                        onRefresh = { viewModel.refreshData(context) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelectorSection(
    currentTheme: Int,
    onThemeSelected: (Int) -> Unit,
    labelColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Personalize Display",
            color = labelColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            val themesList = listOf(
                Triple(0, "Dark", Color(0xFF121214)),
                Triple(1, "Light", Color(0xFFF4F4F5)),
                Triple(2, "Orange", Color(0xFFF7931A)),
                Triple(3, "Matrix", Color(0xFF10B981)),
                Triple(4, "Gold", Color(0xFFC5A059)),
                Triple(5, "Amber", Color(0xFFF59E0B)),
                Triple(6, "GameBoy", Color(0xFF8BAC0F))
            )

            themesList.forEach { (index, name, color) ->
                val isSelected = currentTheme == index
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = color,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color(0xFF8E8E93),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onThemeSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Text(
                            text = "✓",
                            color = if (index == 1) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(titleColor: Color, subtitleColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            text = "SATOSHI DASHBOARD",
            color = titleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Blockclock Micro Console",
            color = subtitleColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
fun DashboardContent(
    data: PriceData,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    cardBgColor: Color,
    borderColor: Color,
    valueColor: Color,
    labelColor: Color,
    subTextColor: Color,
    context: Context,
    prefs: SharedPreferences,
    onSettingsChanged: () -> Unit
) {
    val currency = "USD"
    val symbol = "$"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Offline/Stale Data Warning Card
        if (!data.lastFetchSuccessful) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                border = BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Offline Mode: Showing cached data. Check network status.",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }

        // Grid layout of 8 mock Blockclocks (4 rows of 2 columns)
        
        // Row 1: BTC/USD & SATS/1USD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BlockclockCard(
                labelTop = "BTC",
                labelBottom = currency,
                value = String.format(Locale.US, "%s%.0f", symbol, data.btcUsd),
                subtext = "Market price of bitcoin",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
            BlockclockCard(
                labelTop = "SATS",
                labelBottom = "1$currency",
                value = String.format(Locale.US, "%,d", data.moscowTime),
                subtext = "1$symbol per Satoshis",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Block Height & Moscow Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BlockclockCard(
                labelTop = "",
                labelBottom = "",
                value = String.format(Locale.US, "%.0f", data.blockHeight.toDouble()),
                subtext = "Bitcoin block height",
                showLabel = false,
                showLbl_divider = false,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
            BlockclockCard(
                labelTop = "MSCW",
                labelBottom = "TIME",
                value = String.format(Locale.US, "%d", data.moscowTime),
                subtext = "",
                showLabel = true,
                showLbl_divider = true,
                showSubText = false,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: MSTR BTC (held) & MSTR/BTC (ratio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BlockclockCard(
                labelTop = "MSTR",
                labelBottom = "BTC",
                value = String.format(Locale.US, "%,.0f", data.mstrBtcHeld),
                subtext = "Strategy Inc. - BTC held",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
            BlockclockCard(
                labelTop = "MSTR",
                labelBottom = "BTC",
                value = String.format(Locale.US, "%.5f", data.mstrBtc),
                subtext = "Strategy Inc. - BTC per share",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 4: MSTR/USD & BTC in circulation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BlockclockCard(
                labelTop = "MSTR",
                labelBottom = currency,
                value = String.format(Locale.US, "%s%.0f", symbol, data.mstrUsd),
                subtext = "Strategy Inc. - Share price",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
            val circInMillions = data.btcCirculation / 1_000_000.0
            BlockclockCard(
                labelTop = "BTC",
                labelBottom = "",
                value = String.format(Locale.US, "%.3f", circInMillions),
                suffix = "MIL",
                subtext = "Bitcoin in circulation",
                showLabel = true,
                showLbl_divider = false,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 5: Halving Countdown & Mempool Priority Fees
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val halvingRemaining = 210000 - (data.blockHeight % 210000)
            BlockclockCard(
                labelTop = "HALV",
                labelBottom = "BLKS",
                value = String.format(Locale.US, "%,d", halvingRemaining),
                subtext = "Blocks to next halving",
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
            BlockclockCard(
                labelTop = "FEES",
                labelBottom = "sat/vB",
                value = String.format(Locale.US, "%d", data.feeFastest),
                subtext = String.format(Locale.US, "Mid: %d / Low: %d", data.feeHalfHour, data.feeHour),
                showLabel = true,
                showLbl_divider = true,
                showSubText = true,
                modifier = Modifier.weight(1f),
                cardBgColor = cardBgColor,
                borderColor = borderColor,
                valueColor = valueColor,
                labelColor = labelColor,
                subTextColor = subTextColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Customization Settings Card
        SettingsPanelCard(
            context = context,
            prefs = prefs,
            onSettingsChanged = onSettingsChanged,
            cardBgColor = cardBgColor,
            borderColor = borderColor,
            valueColor = valueColor,
            labelColor = labelColor,
            subTextColor = subTextColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last updated text in monospaced design
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Text(
            text = "Last updated: ${sdf.format(Date(data.timestamp))}",
            color = subTextColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Refresh Button
        RefreshButton(
            onRefresh = onRefresh,
            isLoading = isLoading,
            containerColor = borderColor,
            contentColor = cardBgColor
        )
    }
}

@Composable
fun SettingsPanelCard(
    context: Context,
    prefs: SharedPreferences,
    onSettingsChanged: () -> Unit,
    cardBgColor: Color,
    borderColor: Color,
    valueColor: Color,
    labelColor: Color,
    subTextColor: Color
) {
    // We want to force state updates for checkboxes
    val modeStates = remember {
        mutableStateListOf<Boolean>().apply {
            addAll((0..9).map { prefs.getBoolean("mode_enabled_$it", true) })
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM SETTINGS",
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Mode Customization
            Text(
                text = "Active Widget Modes",
                color = labelColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))

            val modeNames = listOf(
                "BTC Price",
                "Sats per Fiat (Moscow Time)",
                "Block Height",
                "Moscow Time Clock Face",
                "MSTR BTC holdings",
                "MSTR Share-to-BTC ratio",
                "MSTR Share Price",
                "BTC Circulating Supply",
                "Halving Countdown",
                "Mempool Priority Fees"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                modeNames.forEachIndexed { index, name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Must have at least one mode enabled
                                val enabledCount = modeStates.count { it }
                                if (!modeStates[index] || enabledCount > 1) {
                                    val newVal = !modeStates[index]
                                    prefs.edit().putBoolean("mode_enabled_$index", newVal).apply()
                                    modeStates[index] = newVal
                                    onSettingsChanged()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = modeStates[index],
                            onCheckedChange = { checked ->
                                val enabledCount = modeStates.count { it }
                                if (checked || enabledCount > 1) {
                                    prefs.edit().putBoolean("mode_enabled_$index", checked).apply()
                                    modeStates[index] = checked
                                    onSettingsChanged()
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = borderColor,
                                uncheckedColor = labelColor,
                                checkmarkColor = cardBgColor
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = valueColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockclockCard(
    labelTop: String,
    labelBottom: String,
    value: String,
    subtext: String,
    showLabel: Boolean,
    showLbl_divider: Boolean,
    showSubText: Boolean,
    modifier: Modifier = Modifier,
    suffix: String = "",
    cardBgColor: Color = Color(0xFF121214),
    borderColor: Color = Color(0xFFC5A059),
    valueColor: Color = Color(0xFFE4E4E7),
    labelColor: Color = Color(0xFFE4E4E7),
    subTextColor: Color = Color(0xFF8E8E93)
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor), // Matte E-Ink display surface
        border = BorderStroke(1.5.dp, borderColor) // Gold physical device bevel
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main digital content area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showLabel) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = labelTop,
                            color = labelColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        if (labelBottom.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(1.dp)
                                    .background(labelColor)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = labelBottom,
                                color = labelColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            )
                        }
                    }
                }
                // Monospace value stretched/compressed in design
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1
                )

                if (suffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = suffix,
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )
                }
            }

            // Lowercase gray footer description
            Text(
                text = subtext,
                color = subTextColor,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun RefreshButton(
    onRefresh: () -> Unit,
    isLoading: Boolean,
    containerColor: Color = Color(0xFFC5A059),
    contentColor: Color = Color(0xFF121214)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Button(
        onClick = { if (!isLoading) onRefresh() },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .width(180.dp)
            .height(48.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↻",
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = if (isLoading) Modifier.rotate(rotation) else Modifier
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoading) "Refreshing..." else "Refresh",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFC5A059))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fetching market data...",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif
        )
    }
}

@Composable
fun ErrorScreen(error: Throwable, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error.message ?: "An unexpected error occurred",
            color = Color(0xFFEF4444),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        RefreshButton(onRefresh = onRefresh, isLoading = false)
    }
}

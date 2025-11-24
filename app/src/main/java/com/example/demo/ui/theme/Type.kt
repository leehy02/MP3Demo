package com.example.demo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.demo.R


val GmarketSans = FontFamily(
    Font(R.font.gmarketsansttflight, FontWeight.Light),
    Font(R.font.gmarketsansttfmedium, FontWeight.Medium),
    Font(R.font.gmarketsansttfbold, FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = GmarketSans),
    displayMedium = Typography().displayMedium.copy(fontFamily = GmarketSans),
    displaySmall = Typography().displaySmall.copy(fontFamily = GmarketSans),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = GmarketSans),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = GmarketSans),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = GmarketSans),
    titleLarge = Typography().titleLarge.copy(fontFamily = GmarketSans),
    titleMedium = Typography().titleMedium.copy(fontFamily = GmarketSans),
    titleSmall = Typography().titleSmall.copy(fontFamily = GmarketSans),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = GmarketSans),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = GmarketSans),
    bodySmall = Typography().bodySmall.copy(fontFamily = GmarketSans),
    labelLarge = Typography().labelLarge.copy(fontFamily = GmarketSans),
    labelMedium = Typography().labelMedium.copy(fontFamily = GmarketSans),
    labelSmall = Typography().labelSmall.copy(fontFamily = GmarketSans)
)

package com.example.tabelahisabapp.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors - Modern Purple/Pink Gradient Theme
val Purple600 = Color(0xFF6366F1) // Indigo-600 - Main brand color
val Purple700 = Color(0xFF4F46E5) // Indigo-700 - Pressed states
val Purple300 = Color(0xFFA5B4FC) // Indigo-300 - Subtle accents
val Purple50 = Color(0xFFEEF2FF) // Indigo-50 - Background tints
val Pink500 = Color(0xFF764BA2) // Purple-pink - Gradient accent

// Semantic Colors
val SuccessGreen = Color(0xFF10B981) // Emerald-500 - Money received, positive
val DangerRed = Color(0xFFEF4444) // Red-500 - Money owed, negative
val WarningOrange = Color(0xFFF59E0B) // Amber-500 - Alerts, pending actions
val InfoBlue = Color(0xFF3B82F6) // Blue-500 - Information, voice indicators

// Cash Book Section Colors (Four-Section Ledger)
val MoneyReceivedPrimary = Color(0xFF059669) // Emerald-600 - Paisa Aaya header
val MoneyReceivedLight = Color(0xFFD1FAE5) // Emerald-100 - Paisa Aaya background
val MoneyReceivedDark = Color(0xFF047857) // Emerald-700 - Paisa Aaya text

val DailyExpensePrimary = Color(0xFFF59E0B) // Amber-500 - Daily Kharcha header
val DailyExpenseLight = Color(0xFFFEF3C7) // Amber-100 - Daily Kharcha background
val DailyExpenseDark = Color(0xFFD97706) // Amber-600 - Daily Kharcha text

val PurchasePrimary = Color(0xFF7C3AED) // Violet-600 - Purchase header
val PurchaseLight = Color(0xFFEDE9FE) // Violet-100 - Purchase background
val PurchaseDark = Color(0xFF6D28D9) // Violet-700 - Purchase text

val PaymentPrimary = Color(0xFFDC2626) // Red-600 - Payment header
val PaymentLight = Color(0xFFFEE2E2) // Red-100 - Payment background
val PaymentDark = Color(0xFFB91C1C) // Red-700 - Payment text

// Neutral Colors
val BackgroundGray = Color(0xFFF9FAFB) // Gray-50 - App background
val CardBackground = Color(0xFFFFFFFF) // White - Card surfaces
val BorderGray = Color(0xFFE5E7EB) // Gray-200 - Dividers
val TextPrimary = Color(0xFF111827) // Gray-900 - Main text
val TextSecondary = Color(0xFF6B7280) // Gray-500 - Labels, hints

// Gradient Start/End Colors
val GradientPurpleStart = Color(0xFF667EEA) // Gradient header start
val GradientPurpleEnd = Color(0xFF764BA2) // Gradient header end
val GradientGreenStart = Color(0xFF10B981) // Success gradient start
val GradientGreenEnd = Color(0xFF059669) // Success gradient end

// Backward compatibility (for existing code)
val Purple80 = Purple300
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Purple600
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Pink500

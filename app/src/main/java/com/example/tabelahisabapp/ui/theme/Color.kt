package com.example.tabelahisabapp.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════════
// PREMIUM COLOR PALETTE - QURESHI KHATA BOOK
// Supports both Dark Mode and Light Mode
// ═══════════════════════════════════════════════════════════════════════════════

// ──────────────────────────────────────────────────────────────────────────────
// PRIMARY BRAND COLORS
// ──────────────────────────────────────────────────────────────────────────────
val Purple600 = Color(0xFF6366F1) // Indigo-600 - Main brand color
val Purple700 = Color(0xFF4F46E5) // Indigo-700 - Pressed states
val Purple300 = Color(0xFFA5B4FC) // Indigo-300 - Subtle accents
val Purple50 = Color(0xFFEEF2FF)  // Indigo-50 - Background tints
val Pink500 = Color(0xFF764BA2)   // Purple-pink - Gradient accent

// ──────────────────────────────────────────────────────────────────────────────
// SEMANTIC ACCENT COLORS (Used for both themes)
// ──────────────────────────────────────────────────────────────────────────────
val AccentBlue = Color(0xFF3B82F6)    // Blue-500 - Bank, information
val AccentGreen = Color(0xFF10B981)   // Emerald-500 - Money received, positive
val AccentRed = Color(0xFFEF4444)     // Red-500 - Money owed, negative
val AccentOrange = Color(0xFFF97316)  // Orange-500 - Seller pending, warnings
val AccentPurple = Color(0xFF8B5CF6)  // Violet-500 - Charts, special items
val AccentTeal = Color(0xFF14B8A6)    // Teal-500 - Alternative positive

// ──────────────────────────────────────────────────────────────────────────────
// SOFT BACKGROUND COLORS (For cards in Light Mode)
// ──────────────────────────────────────────────────────────────────────────────
val SoftGreen = Color(0xFFD1FAE5)   // Emerald-100 - Money received cards
val SoftRed = Color(0xFFFEE2E2)     // Red-100 - Money owed cards
val SoftBlue = Color(0xFFDBEAFE)    // Blue-100 - Bank transaction cards
val SoftPurple = Color(0xFFEDE9FE)  // Violet-100 - Purchase cards
val SoftOrange = Color(0xFFFED7AA)  // Orange-200 - Warning cards
val SoftTeal = Color(0xFFCCFBF1)    // Teal-100 - Alternative cards

// ──────────────────────────────────────────────────────────────────────────────
// DARK MODE SOFT COLORS (For cards in Dark Mode)
// ──────────────────────────────────────────────────────────────────────────────
val DarkSoftGreen = Color(0xFF064E3B)   // Emerald-900
val DarkSoftRed = Color(0xFF7F1D1D)     // Red-900
val DarkSoftBlue = Color(0xFF1E3A5F)    // Blue-900
val DarkSoftPurple = Color(0xFF4C1D95)  // Violet-900
val DarkSoftOrange = Color(0xFF7C2D12)  // Orange-900
val DarkSoftTeal = Color(0xFF134E4A)    // Teal-900

// ──────────────────────────────────────────────────────────────────────────────
// LIGHT MODE NEUTRAL COLORS
// ──────────────────────────────────────────────────────────────────────────────
val BackgroundGray = Color(0xFFF9FAFB)   // Gray-50 - App background
val CardBackground = Color(0xFFFFFFFF)   // White - Card surfaces
val BorderGray = Color(0xFFE5E7EB)       // Gray-200 - Dividers
val TextPrimary = Color(0xFF111827)      // Gray-900 - Main text
val TextSecondary = Color(0xFF6B7280)    // Gray-500 - Labels, hints
val TextTertiary = Color(0xFF9CA3AF)     // Gray-400 - Disabled text

// ──────────────────────────────────────────────────────────────────────────────
// DARK MODE NEUTRAL COLORS
// ──────────────────────────────────────────────────────────────────────────────
val DarkBackground = Color(0xFF0F172A)       // Slate-900 - App background
val DarkSurface = Color(0xFF1E293B)          // Slate-800 - Card surfaces
val DarkSurfaceVariant = Color(0xFF334155)   // Slate-700 - Elevated surfaces
val DarkBorder = Color(0xFF475569)           // Slate-600 - Dividers
val DarkTextPrimary = Color(0xFFF8FAFC)      // Slate-50 - Main text
val DarkTextSecondary = Color(0xFF94A3B8)    // Slate-400 - Labels, hints
val DarkTextTertiary = Color(0xFF64748B)     // Slate-500 - Disabled text

// ──────────────────────────────────────────────────────────────────────────────
// GRADIENT COLORS
// ──────────────────────────────────────────────────────────────────────────────
val GradientPurpleStart = Color(0xFF667EEA)  // Gradient header start
val GradientPurpleEnd = Color(0xFF764BA2)    // Gradient header end
val GradientGreenStart = Color(0xFF10B981)   // Success gradient start
val GradientGreenEnd = Color(0xFF059669)     // Success gradient end
val GradientBlueStart = Color(0xFF3B82F6)    // Info gradient start
val GradientBlueEnd = Color(0xFF1D4ED8)      // Info gradient end

// Dark mode gradients (more muted)
val DarkGradientPurpleStart = Color(0xFF4338CA)
val DarkGradientPurpleEnd = Color(0xFF5B21B6)

// Chart colors
val ChartLineColor = Color(0xFF6366F1)       // Chart line
val ChartFillStart = Color(0x406366F1)       // Chart area fill start
val ChartFillEnd = Color(0x006366F1)         // Chart area fill end

// ──────────────────────────────────────────────────────────────────────────────
// CASH BOOK SECTION COLORS (Four-Section Ledger)
// ──────────────────────────────────────────────────────────────────────────────
// Money Received (Paisa Aaya)
val MoneyReceivedPrimary = Color(0xFF059669)  // Emerald-600 - Header
val MoneyReceivedLight = Color(0xFFD1FAE5)    // Emerald-100 - Background
val MoneyReceivedDark = Color(0xFF047857)     // Emerald-700 - Text

// Daily Expense (Daily Kharcha)
val DailyExpensePrimary = Color(0xFFF59E0B)   // Amber-500 - Header
val DailyExpenseLight = Color(0xFFFEF3C7)     // Amber-100 - Background
val DailyExpenseDark = Color(0xFFD97706)      // Amber-600 - Text

// Purchase (Khareedari)
val PurchasePrimary = Color(0xFF7C3AED)       // Violet-600 - Header
val PurchaseLight = Color(0xFFEDE9FE)         // Violet-100 - Background
val PurchaseDark = Color(0xFF6D28D9)          // Violet-700 - Text

// Payment (Diya)
val PaymentPrimary = Color(0xFFDC2626)        // Red-600 - Header
val PaymentLight = Color(0xFFFEE2E2)          // Red-100 - Payment background
val PaymentDark = Color(0xFFB91C1C)           // Red-700 - Payment text

// ──────────────────────────────────────────────────────────────────────────────
// LEGACY COMPATIBILITY (for existing code)
// ──────────────────────────────────────────────────────────────────────────────
val SuccessGreen = AccentGreen
val DangerRed = AccentRed
val WarningOrange = AccentOrange
val InfoBlue = AccentBlue

// Material You compatibility
val Purple80 = Purple300
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Purple600
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Pink500

// ──────────────────────────────────────────────────────────────────────────────
// NAVIGATION BAR COLORS
// ──────────────────────────────────────────────────────────────────────────────
val NavBarLight = Color(0xFFFFFFFF)
val NavBarDark = Color(0xFF1E293B)
val NavBarSelectedLight = Purple600
val NavBarSelectedDark = Color(0xFFA5B4FC)
val NavBarUnselectedLight = TextSecondary
val NavBarUnselectedDark = DarkTextSecondary

// Additional UI colors
val Blue500 = AccentBlue
val Orange500 = AccentOrange
val Teal500 = AccentTeal
val Green600 = Color(0xFF059669)
val Purple100 = Color(0xFFE0E7FF)


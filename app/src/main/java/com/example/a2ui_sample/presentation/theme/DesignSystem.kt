package com.example.a2ui_sample.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

object PremiumColors {
    // Primary palette
    val Primary = Color(0xFF000000) // Deep Modern Black
    val Secondary = Color(0xFF1A1A1A)
    val Accent = Color(0xFF6366F1) // Modern Indigo (Stripe/Linear style)
    
    // Backgrounds
    val BackgroundLight = Color(0xFFF9FAFB)
    val SurfaceLight = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF0A0A0A)
    val SurfaceDark = Color(0xFF111111)

    // Semantic Colors
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    // Neutrals
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray800 = Color(0xFF1F2937)
    val Gray900 = Color(0xFF111827)

    // Gradients
    val PremiumGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7))
    )
    val GlassyGradient = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f))
    )
}

object PremiumTypography {
    val HeadlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 40.sp
    )
    
    val HeadlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 32.sp
    )

    val BodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )

    val LabelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    )
}

object PremiumSpacing {
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val Huge = 48.dp
}

object PremiumShapes {
    val CardRadius = 16.dp
    val ButtonRadius = 12.dp
    val InputRadius = 10.dp
}

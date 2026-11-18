package tech.tamaki.alcoholmeter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R

private val Geist = FontFamily(Font(R.font.geist_regular, FontWeight.Normal), Font(R.font.geist_semibold, FontWeight.SemiBold))
val AlcoholTypography = Typography(
    headlineLarge=TextStyle(fontFamily=Geist, fontWeight=FontWeight.SemiBold, fontSize=28.sp, lineHeight=34.sp),
    headlineMedium=TextStyle(fontFamily=Geist, fontWeight=FontWeight.SemiBold, fontSize=24.sp),
    titleLarge=TextStyle(fontFamily=Geist, fontWeight=FontWeight.SemiBold, fontSize=20.sp),
    bodyLarge=TextStyle(fontFamily=Geist, fontSize=16.sp),
    bodyMedium=TextStyle(fontFamily=Geist, fontSize=14.sp),
    labelSmall=TextStyle(fontFamily=Geist, fontSize=11.sp)
)

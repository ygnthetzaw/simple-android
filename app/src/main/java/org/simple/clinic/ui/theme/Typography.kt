package org.simple.clinic.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography.tag
  get() = TextStyle(
      fontWeight = FontWeight.Medium,
      fontSize = 14.sp,
      letterSpacing = 0.0571.sp,
      lineHeight = 20.sp
  )

val Typography.body0
  get() = TextStyle(
      fontWeight = FontWeight.Normal,
      fontSize = 18.sp,
      letterSpacing = 0.011.sp,
      lineHeight = 28.sp
  )

val Typography.body0Medium
  get() = body0.copy(
      fontWeight = FontWeight.Medium,
      letterSpacing = 0.0083.sp,
  )

val Typography.buttonBig
  get() = button.copy(
      fontSize = 16.sp,
      letterSpacing = 0.0781.sp,
      lineHeight = 20.sp
  )

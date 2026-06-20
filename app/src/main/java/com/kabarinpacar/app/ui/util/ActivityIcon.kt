package com.kabarinpacar.app.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.kabarinpacar.app.data.model.ActivityType

data class ActivityVisual(val icon: ImageVector, val tint: Color)

fun ActivityType.visual() = when (this) {
    ActivityType.KERJA      -> ActivityVisual(Icons.Outlined.Work,         Color(0xFF5C6BC0))
    ActivityType.DI_JALAN   -> ActivityVisual(Icons.Outlined.DirectionsCar, Color(0xFF26A69A))
    ActivityType.MAKAN      -> ActivityVisual(Icons.Outlined.Restaurant,    Color(0xFFEF6C00))
    ActivityType.ISTIRAHAT  -> ActivityVisual(Icons.Outlined.Bedtime,       Color(0xFF7E57C2))
    ActivityType.LAINNYA    -> ActivityVisual(Icons.Outlined.MoreHoriz,     Color(0xFF78909C))
}

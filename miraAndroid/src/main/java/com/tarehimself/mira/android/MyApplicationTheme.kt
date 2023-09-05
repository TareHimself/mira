package com.tarehimself.mira.android

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyApplicationTheme(
    context: Context,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
//            dynamicDarkColorScheme(context)
//        }
//        else
//        {
//            darkColorScheme(
//                primary = Color(0xFFBB86FC),
//                onPrimary = Color.White,
//                primaryContainer = Color(0xFF3700B3),
//                secondary = Color(0xFF03DAC5),
//                onSecondary = Color.White,
//
//
////            surface = Color.DarkGray
//            )
//        }


//        darkColorScheme(
//            primary = Color(0xFF1E88E5),
//            onPrimary = Color.White,
//            primaryContainer = Color(0xFF1565C0),
//            onPrimaryContainer = Color.White,
//            inversePrimary = Color(0xFFBB86FC),
//            secondary = Color(0xFF4CAF50),
//            onSecondary = Color.White,
//            secondaryContainer = Color(0xFF388E3C),
//            onSecondaryContainer = Color.White,
//            tertiary = Color(0xFFFFC107),
//            onTertiary = Color.Black,
//            tertiaryContainer = Color(0xFFD39E00),
//            onTertiaryContainer = Color.White,
//            background = Color(0xFF121212),
//            onBackground = Color.White,
//            surface = Color(0xFF121212),
//            onSurface = Color.White,
//            surfaceVariant = Color(0xFF424242),
//            onSurfaceVariant = Color.White,
//            surfaceTint = Color(0xFF1E88E5),
//            inverseSurface = Color(0xFF1E88E5),
//            inverseOnSurface = Color.White,
//            error = Color(0xFFD32F2F),
//            onError = Color.White,
//            errorContainer = Color(0xFF9A0007),
//            onErrorContainer = Color.White,
//            outline = Color(0xFFBDBDBD),
//            outlineVariant = Color(0xFF757575),
//            scrim = Color(0x99000000),
//        )
    } else {
//        lightColorScheme(
//            primary = Color(0xFF6200EE),
//            primaryContainer = Color(0xFF3700B3),
//            secondary = Color(0xFF03DAC5)
//        )
//        lightColorScheme(
//            primary = Color(0xFF1E88E5),
//            onPrimary = Color.White,
//            primaryContainer = Color(0xFF1565C0),
//            onPrimaryContainer = Color.White,
//            inversePrimary = Color(0xFFBB86FC),
//            secondary = Color(0xFF4CAF50),
//            onSecondary = Color.White,
//            secondaryContainer = Color(0xFF388E3C),
//            onSecondaryContainer = Color.White,
//            tertiary = Color(0xFFFFC107),
//            onTertiary = Color.Black,
//            tertiaryContainer = Color(0xFFD39E00),
//            onTertiaryContainer = Color.White,
//            background = Color(0xFFF5F5F5), // A softer background color
//            onBackground = Color.Black,
//            surface = Color(0xFFFFFFFF), // White surface
//            onSurface = Color.Black,
//            surfaceVariant = Color(0xFFF0F0F0), // Slightly darker surface variant
//            onSurfaceVariant = Color.Black,
//            surfaceTint = Color(0xFF1E88E5),
//            inverseSurface = Color(0xFF1E88E5),
//            inverseOnSurface = Color.White,
//            error = Color(0xFFD32F2F),
//            onError = Color.White,
//            errorContainer = Color(0xFF9A0007),
//            onErrorContainer = Color.White,
//            outline = Color(0xFFBDBDBD),
//            outlineVariant = Color(0xFF757575),
//            scrim = Color(0x99000000),
//        )
        LightColors
    }
    val typography = Typography()


//    bodySmall = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp
//    )

    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = shapes,
        content = content,
    )
}

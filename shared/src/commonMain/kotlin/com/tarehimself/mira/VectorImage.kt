package com.tarehimself.mira

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter


@Composable
fun VectorImage(vector: ImageVector,
                modifier: Modifier = Modifier,
                color: Color = Color.White, contentDescription: String) {

    Image(
        painter = rememberVectorPainter(defaultWidth = vector.defaultWidth,
            defaultHeight = vector.defaultHeight,
            viewportWidth = vector.viewportWidth,
            viewportHeight = vector.viewportHeight,
            name = vector.name,
            tintColor = color,
            tintBlendMode = vector.tintBlendMode,
            autoMirror = vector.autoMirror,
            content = { _, _ -> RenderVectorGroup(group = vector.root) }),
        contentDescription = contentDescription,
        modifier = modifier
        )
}
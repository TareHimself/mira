import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.VectorImage
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.launch


@Composable
fun BottomSheetIcon(vector: ImageVector, contentDescription: String, onClick: () -> Unit, iconSize: Dp = 20.dp, onLongClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit = {}){
    Pressable(
        modifier = Modifier.fillMaxHeight().aspectRatio(1.0f).borderRadius(5.dp),
        backgroundColor = Color.Transparent,
        onClick = onClick,
        onLongClick =  onLongClick
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            VectorImage(
                vector = vector,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize).align(Alignment.Center)
            )
            content()
        }
    }
}

@Composable
fun BottomSheetIconRow(content: @Composable RowScope.() -> Unit){
    Surface(
        modifier = Modifier.height(80.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = ModalBottomSheetDefaults.Elevation
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
package com.tarehimself.mira.manga.viewer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.StoredChaptersRead
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.rememberReadInfo
import compose.icons.Octicons
import compose.icons.octicons.Download16


@Composable
fun MangaChapterContent(
    component: MangaViewerComponent,
    index: Int,
    total: Int,
    sourceId: String,
    mangaId: String,
    data: MangaChapter,
    isSelected: Boolean,
    onChapterSelected: (data: MangaChapter) -> Unit,
    onChapterLongPressed: (data: MangaChapter) -> Unit
) {

    val itemHeight by remember { mutableStateOf(60.dp) }

    var itemWidth by remember { mutableStateOf(0) }

    val itemWidthDp = itemWidth.pxToDp()

    val readInfo = rememberReadInfo(sourceId,mangaId)

    val hasBeenRead = readInfo?.read?.contains(total - 1 - index) == true

    val backgroundColor by animateColorAsState(
        when (isSelected) {
            true -> Color.Gray
            else -> Color.Transparent
        }
    )

    Pressable(modifier = Modifier.fillMaxWidth().height(itemHeight), onClick = {
        onChapterSelected(data)
    }, onLongClick = {
        onChapterLongPressed(data)
    },
        backgroundColor = backgroundColor
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp).onGloballyPositioned {
                itemWidth = it.size.width
            }
        ) {
            Box(
                modifier = Modifier.width(itemWidthDp - 30.dp - 20.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        data.name,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = when (hasBeenRead && !isSelected) {
                            true -> Color.DarkGray
                            else -> Color.White
                        }
                    )
                    Row {
                        Text(
                            when (val released = data.released) {
                                is String -> released
                                else -> "Unknown"
                            },
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.DarkGray
                        )
                        when(readInfo){
                            is StoredChaptersRead -> {

                                if((component.state.value.chapters.lastIndex - readInfo.current!!.index) == index){
                                    Text(
                                        "  |  ",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        "Page ${readInfo.current!!.progress}",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                            else -> {

                            }
                        }

                    }

                }
            }

            if (!isSelected) {
                Pressable(modifier = Modifier.size(30.dp), onClick = {
                }) {

                    VectorImage(
                        vector = Octicons.Download16,
                        contentDescription = "Download Chapter"
                    )
                }

            }
        }
    }
}
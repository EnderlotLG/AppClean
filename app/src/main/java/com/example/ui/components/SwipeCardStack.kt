package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SwipeCardStack(
    items: List<MediaItem>,
    canUndo: Boolean,
    onSwipeLeft: (MediaItem) -> Unit,
    onSwipeRight: (MediaItem) -> Unit,
    onUndo: () -> Unit,
    onShowDetail: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val topItem = items.firstOrNull()
    val nextItem = if (items.size > 1) items[1] else null
    val thirdItem = if (items.size > 2) items[2] else null

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Animated Drag Offsets for the Top Card
    val offsetX = remember(topItem?.id) { Animatable(0f) }
    val offsetY = remember(topItem?.id) { Animatable(0f) }

    val swipeThreshold = with(density) { 130.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (topItem == null) {
            // Handled outside or empty placeholder
            return@Box
        }

        // 3rd Card in background
        if (thirdItem != null) {
            MediaCardView(
                item = thirdItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(490.dp)
                    .offset(y = 24.dp)
                    .scale(0.88f)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                isTopCard = false
            )
        }

        // 2nd Card in background
        if (nextItem != null) {
            val dragProgress = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
            val nextScale by animateFloatAsState(
                targetValue = 0.94f + (0.06f * dragProgress),
                label = "nextCardScale"
            )
            val nextOffsetY by animateFloatAsState(
                targetValue = 12f * (1f - dragProgress),
                label = "nextCardOffset"
            )

            MediaCardView(
                item = nextItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(490.dp)
                    .offset(y = nextOffsetY.dp)
                    .scale(nextScale)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                isTopCard = false
            )
        }

        // 1st Card (Top Card with Interactive Gestures)
        val rotationAngle = (offsetX.value / 25f).coerceIn(-20f, 20f)
        val dragFraction = (offsetX.value / swipeThreshold).coerceIn(-1.5f, 1.5f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(490.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .rotate(rotationAngle)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(topItem.id) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -swipeThreshold) {
                                    // Swipe Left -> Delete
                                    launch {
                                        offsetX.animateTo(
                                            targetValue = -1200f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        onSwipeLeft(topItem)
                                    }
                                } else if (offsetX.value > swipeThreshold) {
                                    // Swipe Right -> Keep
                                    launch {
                                        offsetX.animateTo(
                                            targetValue = 1200f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        onSwipeRight(topItem)
                                    }
                                } else {
                                    // Snap back
                                    launch {
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                    launch {
                                        offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f)
                                offsetY.animateTo(0f)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y * 0.4f)
                            }
                        }
                    )
                }
        ) {
            MediaCardView(
                item = topItem,
                modifier = Modifier.fillMaxSize(),
                isTopCard = true,
                onInfoClick = { onShowDetail(topItem) }
            )

            // Dynamic "ELIMINAR" Overlay Badge (Left Drag)
            if (dragFraction < 0f) {
                val alpha = (-dragFraction).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .rotate(15f)
                        .background(
                            Color(0xFFDC2626).copy(alpha = 0.92f * alpha),
                            RoundedCornerShape(12.dp)
                        )
                        .border(2.dp, Color.White.copy(alpha = alpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = alpha),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ELIMINAR",
                            color = Color.White.copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Dynamic "CONSERVAR" Overlay Badge (Right Drag)
            if (dragFraction > 0f) {
                val alpha = dragFraction.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .rotate(-15f)
                        .background(
                            Color(0xFF16A34A).copy(alpha = 0.92f * alpha),
                            RoundedCornerShape(12.dp)
                        )
                        .border(2.dp, Color.White.copy(alpha = alpha), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = alpha),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "CONSERVAR",
                            color = Color.White.copy(alpha = alpha),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Floating Quick Action Controls Row (Tinder-style buttons)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo Button
            OutlinedIconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("undo_button"),
                shape = CircleShape,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Deshacer",
                    tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Reject / Delete Button (Swipe Left)
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(-1200f, spring(stiffness = Spring.StiffnessMediumLow))
                        onSwipeLeft(topItem)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .testTag("delete_swipe_button"),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFEF4444)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Eliminar foto o video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Info / Zoom Button
            OutlinedIconButton(
                onClick = { onShowDetail(topItem) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("info_button"),
                shape = CircleShape,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Detalles",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Keep / Favorite Button (Swipe Right)
            FilledIconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(1200f, spring(stiffness = Spring.StiffnessMediumLow))
                        onSwipeRight(topItem)
                    }
                },
                modifier = Modifier
                    .size(64.dp)
                    .testTag("keep_swipe_button"),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Conservar foto o video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun MediaCardView(
    item: MediaItem,
    modifier: Modifier = Modifier,
    isTopCard: Boolean = true,
    onInfoClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val formattedDate = remember(item.dateAddedMs) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(item.dateAddedMs))
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Thumbnail / Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video Indicator Overlay in Center
            if (item.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    )
                }
            }

            // Top Gradient & Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (item.isVideo) Icons.Default.Videocam else Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Text(
                                text = item.bucketName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Size Badge (High visual prominence)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (item.sizeBytes > 20 * 1024 * 1024L) Color(0xFFEA580C) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (item.sizeBytes > 20 * 1024 * 1024L) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = item.formattedSize,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom Gradient & Media Metadata
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 80.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.displayName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.formattedDuration.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Red.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = item.formattedDuration,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formattedDate,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                        if (item.width > 0 && item.height > 0) {
                            Text(
                                text = "${item.width} × ${item.height}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

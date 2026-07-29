package com.partitionsoft.stacksy.game.presentation

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.partitionsoft.stacksy.R
import com.partitionsoft.stacksy.game.domain.ACTIVE_SPAWN_BOTTOM
import com.partitionsoft.stacksy.game.domain.GamePiece
import com.partitionsoft.stacksy.game.domain.GameStatus
import com.partitionsoft.stacksy.game.domain.GameUiState
import com.partitionsoft.stacksy.game.domain.PieceKind
import com.partitionsoft.stacksy.game.domain.PieceMotion
import com.partitionsoft.stacksy.game.domain.SPAWN_GAP
import com.partitionsoft.stacksy.game.domain.towerCenterForScreenPosition
import kotlinx.coroutines.isActive
import kotlin.math.max

@Composable
fun GameBoard(
    uiState: GameUiState,
    onFrame: (Float) -> Unit,
    onDrop: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val cameraOffset = remember { Animatable(0f) }
    val dropDescription = stringResource(R.string.drop_piece)
    val towerTop = uiState.stack.maxOfOrNull { it.top } ?: 0f
    val sceneTop = uiState.activePiece?.piece?.let { active ->
        max(ACTIVE_SPAWN_BOTTOM, towerTop + SPAWN_GAP) + active.height
    } ?: towerTop
    val boardZoom = 1f
    val targetCameraOffset = if (boardSize.width > 0) {
        val viewportHeight = boardSize.height * (GROUND_FRACTION - PLAYABLE_TOP_FRACTION)
        val visibleWorldHeight = viewportHeight / (boardSize.width * boardZoom)
        max(0f, sceneTop - visibleWorldHeight)
    } else {
        0f
    }
    val movingScreenCenter = uiState.activePiece
        ?.takeIf { it.motion == PieceMotion.Moving }
        ?.let { movingScreenCenter(it.piece, boardZoom) }
    val emojiPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
    }
    LaunchedEffect(targetCameraOffset) {
        if (targetCameraOffset > cameraOffset.value && uiState.stack.size > 1) {
            cameraOffset.animateTo(
                targetValue = targetCameraOffset,
                animationSpec = tween(
                    durationMillis = CAMERA_PAN_DURATION_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
            )
        } else {
            cameraOffset.snapTo(targetCameraOffset)
        }
    }
    LaunchedEffect(uiState.status) {
        if (uiState.status != GameStatus.Playing) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frame ->
                onFrame((frame - previousFrame) / 1_000_000_000f)
                previousFrame = frame
            }
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { boardSize = it }
            .semantics { contentDescription = dropDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = uiState.status == GameStatus.Playing,
                onClick = {
                    movingScreenCenter?.let { screenCenter ->
                        onDrop(towerCenterForScreenPosition(screenCenter, boardZoom))
                    }
                },
            )
            .testTag("game_board"),
    ) {
        val playableTop = size.height * PLAYABLE_TOP_FRACTION
        val groundY = size.height * GROUND_FRACTION
        val scale = size.width * boardZoom
        val xOffset = (size.width - scale) / 2f

        clipRect(top = playableTop, bottom = groundY + 2f) {
            drawLine(
                color = Color(0x553F334D),
                start = Offset(xOffset, groundY + cameraOffset.value * scale),
                end = Offset(xOffset + scale, groundY + cameraOffset.value * scale),
                strokeWidth = 4f,
            )
            uiState.stack.forEach { piece ->
                drawPiece(piece, groundY, cameraOffset.value, scale, xOffset, emojiPaint)
            }
            uiState.activePiece?.let { active ->
                drawPiece(
                    piece = active.piece,
                    groundY = groundY,
                    cameraOffset = cameraOffset.value,
                    scale = scale,
                    xOffset = xOffset,
                    emojiPaint = emojiPaint,
                    movingCenterX = if (active.motion == PieceMotion.Moving) {
                        movingScreenCenter?.times(size.width)
                    } else null,
                )
            }
        }
    }
}

private fun DrawScope.drawPiece(
    piece: GamePiece,
    groundY: Float,
    cameraOffset: Float,
    scale: Float,
    xOffset: Float,
    emojiPaint: Paint,
    movingCenterX: Float? = null,
) {
    val width = piece.width * scale
    val height = piece.height * scale
    val centerX = movingCenterX ?: xOffset + piece.centerX * scale
    val left = centerX - width / 2f
    val top = groundY - (piece.top - cameraOffset) * scale
    val color = pieceColor(piece.kind)

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.15f),
        topLeft = Offset(left + 4f, top + 7f),
        size = Size(width, height),
        cornerRadius = CornerRadius(height * 0.28f),
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(height * 0.28f),
    )
    emojiPaint.textSize = height * 0.76f
    drawContext.canvas.nativeCanvas.drawText(
        piece.kind.emoji,
        centerX,
        top + height * 0.76f,
        emojiPaint,
    )
}

private fun movingScreenCenter(piece: GamePiece, boardZoom: Float): Float {
    val worldHalfWidth = piece.width / 2f
    val progress = ((piece.centerX - worldHalfWidth) / (1f - piece.width)).coerceIn(0f, 1f)
    val screenHalfWidth = worldHalfWidth * boardZoom
    return screenHalfWidth + progress * (1f - screenHalfWidth * 2f)
}

private const val PLAYABLE_TOP_FRACTION = 0.17f
private const val GROUND_FRACTION = 0.84f
private const val CAMERA_PAN_DURATION_MILLIS = 380

private fun pieceColor(kind: PieceKind): Color = when (kind) {
    PieceKind.Basket -> Color(0xFFC98B52)
    PieceKind.Donut -> Color(0xFFFF91B5)
    PieceKind.Burger -> Color(0xFFFFB24A)
    PieceKind.Cheese -> Color(0xFFFFD85A)
    PieceKind.Cupcake -> Color(0xFFB58CFF)
    PieceKind.Pizza -> Color(0xFFFF8C42)
    PieceKind.Fries -> Color(0xFFFFC857)
    PieceKind.Cookie -> Color(0xFFD99A5B)
    PieceKind.Watermelon -> Color(0xFF5CCB7A)
    PieceKind.Present -> Color(0xFFFF6B6B)
    PieceKind.Sushi -> Color(0xFFFF9E80)
    PieceKind.RiceBall -> Color(0xFFE8E4DC)
    PieceKind.Shrimp -> Color(0xFFFF8A80)
    PieceKind.Ramen -> Color(0xFFFFC857)
    PieceKind.Bento -> Color(0xFFB07A5A)
    PieceKind.Cake -> Color(0xFFFFA8C5)
    PieceKind.Candy -> Color(0xFF80DEEA)
    PieceKind.IceCream -> Color(0xFFCE93D8)
    PieceKind.Chocolate -> Color(0xFF8D6E63)
    PieceKind.Lollipop -> Color(0xFFFF80AB)
    PieceKind.Pineapple -> Color(0xFFFFD54F)
    PieceKind.Mango -> Color(0xFFFFB74D)
    PieceKind.Coconut -> Color(0xFFA1887F)
    PieceKind.Banana -> Color(0xFFFFEE58)
    PieceKind.Kiwi -> Color(0xFF9CCC65)
}

package com.partitionsoft.stacksy.game.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val BASE_HORIZONTAL_SPEED = 0.34f
const val DROP_SPEED = 1.15f
const val ACTIVE_SPAWN_BOTTOM = 1.03f
const val SPAWN_GAP = 0.32f
const val STABILITY_TOLERANCE_RATIO = 0.04f

fun overlapWidth(first: GamePiece, second: GamePiece): Float =
    (min(first.right, second.right) - max(first.left, second.left)).coerceAtLeast(0f)

fun difficultyForScore(score: Int): Float = when {
    score < 200 -> 1f
    score < 500 -> 2f
    score < 1_000 -> 3f
    score < 2_000 -> 4f
    score < 3_000 -> 5f
    else -> 6f
}

fun visibleSpeedStep(difficulty: Float): Int = (difficulty * 10).roundToInt()

fun spawnBottomFor(towerTop: Float): Float =
    max(ACTIVE_SPAWN_BOTTOM, towerTop + SPAWN_GAP)

fun towerCenterForScreenPosition(screenCenterX: Float, boardZoom: Float): Float {
    val safeZoom = boardZoom.coerceIn(0.38f, 1f)
    return 0.5f + (screenCenterX - 0.5f) / safeZoom
}

fun resolvePlacement(
    dropped: GamePiece,
    existingStack: List<GamePiece>,
    previousCombo: Int,
): PlacementResult? {
    val support = existingStack.lastOrNull() ?: return null
    val overlap = overlapWidth(dropped, support)
    if (overlap <= 0f) return null

    val perfectTolerance = min(dropped.width, support.width) * 0.06f
    val perfect = abs(dropped.centerX - support.centerX) <= perfectTolerance
    val landed = dropped.copy(
        centerX = if (perfect) support.centerX else dropped.centerX,
        bottom = support.top,
    )
    val overlapRatio = (overlap / min(dropped.width, support.width)).coerceIn(0f, 1f)
    val combo = if (perfect) previousCombo + 1 else 0
    val multiplier = if (perfect) (combo + 1).coerceAtMost(5) else 1
    val points = (10 + overlapRatio * 20).roundToInt() * multiplier
    val cumulativeStability = towerStability(existingStack + landed)

    return PlacementResult(
        landedPiece = landed,
        overlapRatio = overlapRatio,
        perfect = perfect,
        combo = combo,
        multiplier = multiplier,
        points = points,
        stability = cumulativeStability.margin,
    )
}

fun towerStability(stack: List<GamePiece>): TowerStability {
    if (stack.size < 2) return TowerStability(stable = true, margin = 1f)

    var smallestMargin = 1f
    for (upperIndex in 1 until stack.size) {
        val upper = stack[upperIndex]
        val support = stack[upperIndex - 1]
        val contactLeft = max(upper.left, support.left)
        val contactRight = min(upper.right, support.right)
        if (contactRight <= contactLeft) {
            return TowerStability(false, 0f, failingUpperIndex = upperIndex)
        }

        val piecesAbove = stack.subList(upperIndex, stack.size)
        val totalMass = piecesAbove.sumOf { (it.width * it.height).toDouble() }.toFloat()
        val centerOfMass = piecesAbove.sumOf {
            (it.centerX * it.width * it.height).toDouble()
        }.toFloat() / totalMass
        val tolerance = min(upper.width, support.width) * STABILITY_TOLERANCE_RATIO
        if (centerOfMass !in (contactLeft - tolerance)..(contactRight + tolerance)) {
            return TowerStability(false, 0f, failingUpperIndex = upperIndex)
        }

        val halfContact = (contactRight - contactLeft) / 2f
        val margin = if (halfContact == 0f) 0f else
            min(centerOfMass - contactLeft, contactRight - centerOfMass) / halfContact
        smallestMargin = min(smallestMargin, margin.coerceIn(0f, 1f))
    }
    return TowerStability(stable = true, margin = smallestMargin)
}

fun pieceSize(kind: PieceKind): Pair<Float, Float> = when (kind) {
    PieceKind.Basket -> 0.40f to 0.10f
    PieceKind.Burger -> 0.25f to 0.11f
    PieceKind.Cheese -> 0.22f to 0.10f
    PieceKind.Cupcake -> 0.20f to 0.12f
    PieceKind.Donut -> 0.21f to 0.10f
    PieceKind.Pizza -> 0.24f to 0.10f
    PieceKind.Fries -> 0.19f to 0.12f
    PieceKind.Cookie -> 0.20f to 0.10f
    PieceKind.Watermelon -> 0.23f to 0.11f
    PieceKind.Present -> 0.20f to 0.12f
    PieceKind.Sushi -> 0.21f to 0.10f
    PieceKind.RiceBall -> 0.20f to 0.12f
    PieceKind.Shrimp -> 0.23f to 0.10f
    PieceKind.Ramen -> 0.25f to 0.12f
    PieceKind.Bento -> 0.24f to 0.11f
    PieceKind.Cake -> 0.23f to 0.12f
    PieceKind.Candy -> 0.19f to 0.10f
    PieceKind.IceCream -> 0.20f to 0.13f
    PieceKind.Chocolate -> 0.22f to 0.10f
    PieceKind.Lollipop -> 0.18f to 0.13f
    PieceKind.Pineapple -> 0.21f to 0.13f
    PieceKind.Mango -> 0.21f to 0.11f
    PieceKind.Coconut -> 0.21f to 0.11f
    PieceKind.Banana -> 0.24f to 0.10f
    PieceKind.Kiwi -> 0.20f to 0.10f
}

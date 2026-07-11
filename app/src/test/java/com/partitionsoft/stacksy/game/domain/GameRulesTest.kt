package com.partitionsoft.stacksy.game.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {
    @Test
    fun missingSupportEndsPlacement() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val dropped = piece(id = 1, centerX = 0.9f, width = 0.2f, bottom = base.top)

        assertNull(resolvePlacement(dropped, listOf(base), previousCombo = 0))
    }

    @Test
    fun anyPositiveOverlapIsAccepted() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val edgePlacement = piece(id = 1, centerX = 0.78f, width = 0.2f, bottom = base.top)

        assertTrue(overlapWidth(edgePlacement, base) > 0f)
        assertTrue(resolvePlacement(edgePlacement, listOf(base), previousCombo = 0) != null)
    }

    @Test
    fun perfectPlacementSnapsAndBuildsComboMultiplier() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val dropped = piece(id = 1, centerX = 0.505f, width = 0.2f, bottom = base.top)

        val result = requireNotNull(resolvePlacement(dropped, listOf(base), previousCombo = 2))

        assertTrue(result.perfect)
        assertEquals(0.5f, result.landedPiece.centerX, 0.0001f)
        assertEquals(3, result.combo)
        assertEquals(4, result.multiplier)
        assertEquals(120, result.points)
    }

    @Test
    fun ordinaryPlacementResetsCombo() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val dropped = piece(id = 1, centerX = 0.61f, width = 0.2f, bottom = base.top)

        val result = requireNotNull(resolvePlacement(dropped, listOf(base), previousCombo = 4))

        assertFalse(result.perfect)
        assertEquals(0, result.combo)
        assertEquals(1, result.multiplier)
        assertTrue(result.points in 10..30)
    }

    @Test
    fun centerOfMassDetectsCumulativeCollapse() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val middle = piece(id = 1, centerX = 0.65f, width = 0.2f, bottom = base.top)
        val top = piece(id = 2, centerX = 0.78f, width = 0.2f, bottom = middle.top)

        val result = towerStability(listOf(base, middle, top))

        assertFalse(result.stable)
        assertEquals(0f, result.margin, 0f)
        assertTrue(result.failingUpperIndex != null)
    }

    @Test
    fun wellSupportedTopPlacementDoesNotInheritAnOldCumulativeImbalance() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val middle = piece(id = 1, centerX = 0.65f, width = 0.2f, bottom = base.top)
        val upper = piece(id = 2, centerX = 0.73f, width = 0.2f, bottom = middle.top)
        val leaningTop = piece(id = 3, centerX = 0.79f, width = 0.2f, bottom = upper.top)
        val dropped = piece(id = 4, centerX = 0.79f, width = 0.2f, bottom = leaningTop.top)
        val existingStack = listOf(base, middle, upper, leaningTop)

        assertFalse(towerStability(existingStack).stable)
        val placement = requireNotNull(resolvePlacement(dropped, existingStack, previousCombo = 0))
        assertEquals(0f, placement.stability, 0f)
    }

    @Test
    fun difficultyUsesTheConfiguredScoreBands() {
        assertEquals(1f, difficultyForScore(0), 0f)
        assertEquals(1f, difficultyForScore(199), 0f)
        assertEquals(2f, difficultyForScore(200), 0f)
        assertEquals(2f, difficultyForScore(499), 0f)
        assertEquals(3f, difficultyForScore(500), 0f)
        assertEquals(3f, difficultyForScore(999), 0f)
        assertEquals(4f, difficultyForScore(1_000), 0f)
        assertEquals(4f, difficultyForScore(1_999), 0f)
        assertEquals(5f, difficultyForScore(2_000), 0f)
        assertEquals(5f, difficultyForScore(2_999), 0f)
        assertEquals(6f, difficultyForScore(3_000), 0f)
        assertEquals(6f, difficultyForScore(100_000), 0f)
    }

    @Test
    fun spawnHeightAlwaysClearsTheCurrentTower() {
        assertEquals(ACTIVE_SPAWN_BOTTOM, spawnBottomFor(towerTop = 0.3f), 0f)
        assertEquals(1.52f, spawnBottomFor(towerTop = 1.2f), 0.0001f)
    }

    @Test
    fun speedNotificationUsesTheVisibleTenthStep() {
        assertEquals(10, visibleSpeedStep(1.04f))
        assertEquals(11, visibleSpeedStep(1.05f))
        assertEquals(18, visibleSpeedStep(1.84f))
    }

    @Test
    fun screenPositionMapsBackIntoTheZoomedTowerWithoutJumping() {
        val zoom = 0.4f
        val screenCenter = 0.62f
        val towerCenter = towerCenterForScreenPosition(screenCenter, zoom)
        val renderedCenter = (1f - zoom) / 2f + towerCenter * zoom

        assertEquals(screenCenter, renderedCenter, 0.0001f)
    }

    @Test
    fun tinyCenterOfMassOverhangUsesBalanceTolerance() {
        val base = piece(id = 0, centerX = 0.5f, width = 0.4f)
        val barelyOutside = piece(id = 1, centerX = 0.705f, width = 0.2f, bottom = base.top)
        val clearlyOutside = barelyOutside.copy(centerX = 0.72f)

        assertTrue(towerStability(listOf(base, barelyOutside)).stable)
        val unstable = towerStability(listOf(base, clearlyOutside))
        assertFalse(unstable.stable)
        assertEquals(1, unstable.failingUpperIndex)
    }

    private fun piece(
        id: Int,
        centerX: Float,
        width: Float,
        bottom: Float = 0f,
    ) = GamePiece(
        id = id,
        kind = if (id == 0) PieceKind.Basket else PieceKind.Donut,
        centerX = centerX,
        bottom = bottom,
        width = width,
        height = 0.1f,
    )
}

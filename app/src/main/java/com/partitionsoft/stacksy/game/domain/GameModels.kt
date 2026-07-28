package com.partitionsoft.stacksy.game.domain

import com.partitionsoft.stacksy.collection.domain.SnackSet

sealed interface GameStatus {
    data object Ready : GameStatus
    data object Playing : GameStatus
    data object Paused : GameStatus
    data object GameOver : GameStatus
}

enum class PieceKind {
    Basket,
    Donut,
    Burger,
    Cheese,
    Cupcake,
    Pizza,
    Fries,
    Cookie,
    Watermelon,
    Present,
    Sushi,
    RiceBall,
    Shrimp,
    Ramen,
    Bento,
    Cake,
    Candy,
    IceCream,
    Chocolate,
    Lollipop,
    Pineapple,
    Mango,
    Coconut,
    Banana,
    Kiwi,

    ;

    val emoji: String
        get() = when (this) {
            Basket -> "🧺"
            Donut -> "🍩"
            Burger -> "🍔"
            Cheese -> "🧀"
            Cupcake -> "🧁"
            Pizza -> "🍕"
            Fries -> "🍟"
            Cookie -> "🍪"
            Watermelon -> "🍉"
            Present -> "🎁"
            Sushi -> "🍣"
            RiceBall -> "🍙"
            Shrimp -> "🍤"
            Ramen -> "🍜"
            Bento -> "🍱"
            Cake -> "🍰"
            Candy -> "🍬"
            IceCream -> "🍦"
            Chocolate -> "🍫"
            Lollipop -> "🍭"
            Pineapple -> "🍍"
            Mango -> "🥭"
            Coconut -> "🥥"
            Banana -> "🍌"
            Kiwi -> "🥝"
        }
}

enum class PieceMotion { Moving, Falling }

enum class PlacementMessage { Unstable }

sealed interface GameEffect {
    data object Drop : GameEffect
    data object Perfect : GameEffect
    data object GameOver : GameEffect
}

data class GamePiece(
    val id: Int,
    val kind: PieceKind,
    val centerX: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
) {
    val left: Float get() = centerX - width / 2f
    val right: Float get() = centerX + width / 2f
    val top: Float get() = bottom + height
}

data class ActivePiece(
    val piece: GamePiece,
    val direction: Float,
    val motion: PieceMotion = PieceMotion.Moving,
)

data class PlacementResult(
    val landedPiece: GamePiece,
    val overlapRatio: Float,
    val perfect: Boolean,
    val combo: Int,
    val multiplier: Int,
    val points: Int,
    val stability: Float,
)

data class TowerStability(
    val stable: Boolean,
    val margin: Float,
    val failingUpperIndex: Int? = null,
)

data class GameCelebration(
    val id: Int,
    val comboMultiplier: Int? = null,
    val speed: Float? = null,
)

data class GameUiState(
    val status: GameStatus = GameStatus.Ready,
    val score: Int = 0,
    val highScore: Int = 0,
    val combo: Int = 0,
    val multiplier: Int = 1,
    val difficulty: Float = 1f,
    val stability: Float = 1f,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val selectedSnackSet: SnackSet = SnackSet.Classic,
    val activeSnackSet: SnackSet = SnackSet.Classic,
    val snackSetUses: Map<SnackSet, Int> = emptyMap(),
    val continueUsed: Boolean = false,
    val newHighScore: Boolean = false,
    val stack: List<GamePiece> = emptyList(),
    val activePiece: ActivePiece? = null,
    val placementMessage: PlacementMessage? = null,
    val celebration: GameCelebration? = null,
)

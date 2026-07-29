package com.partitionsoft.stacksy.game.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.partitionsoft.stacksy.collection.domain.REWARDED_SET_RUNS
import com.partitionsoft.stacksy.collection.domain.SnackSet
import com.partitionsoft.stacksy.collection.domain.canPlaySnackSet
import com.partitionsoft.stacksy.core.preferences.PreferencesStore
import com.partitionsoft.stacksy.game.domain.ActivePiece
import com.partitionsoft.stacksy.game.domain.BASE_HORIZONTAL_SPEED
import com.partitionsoft.stacksy.game.domain.DROP_SPEED
import com.partitionsoft.stacksy.game.domain.GameEffect
import com.partitionsoft.stacksy.game.domain.GameCelebration
import com.partitionsoft.stacksy.game.domain.GamePiece
import com.partitionsoft.stacksy.game.domain.GameStatus
import com.partitionsoft.stacksy.game.domain.GameUiState
import com.partitionsoft.stacksy.game.domain.PieceKind
import com.partitionsoft.stacksy.game.domain.PieceMotion
import com.partitionsoft.stacksy.game.domain.PlacementMessage
import com.partitionsoft.stacksy.game.domain.difficultyForScore
import com.partitionsoft.stacksy.game.domain.pieceSize
import com.partitionsoft.stacksy.game.domain.resolvePlacement
import com.partitionsoft.stacksy.game.domain.spawnBottomFor
import com.partitionsoft.stacksy.game.domain.visibleSpeedStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

class GameViewModel(
    private val preferencesStore: PreferencesStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 4)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private var nextPieceId = 1
    private var nextCelebrationId = 1
    private var resumeAfterLifecycle = false
    private var messageJob: Job? = null
    private var startGameJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesStore.preferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        highScore = max(it.highScore, preferences.highScore),
                        soundEnabled = preferences.soundEnabled,
                        vibrationEnabled = preferences.vibrationEnabled,
                        selectedSnackSet = preferences.selectedSnackSet,
                        snackSetUses = preferences.snackSetUses,
                    )
                }
            }
        }
    }

    fun startGame() {
        if (startGameJob?.isActive == true) return
        startGameJob = viewModelScope.launch {
            startGame(preferencesStore.consumeSelectedSnackSetRun())
        }
    }

    private fun startGame(snackSet: SnackSet) {
        messageJob?.cancel()
        nextPieceId = 1
        nextCelebrationId = 1
        resumeAfterLifecycle = false
        val base = GamePiece(
            id = 0,
            kind = PieceKind.Basket,
            centerX = 0.5f,
            bottom = 0.03f,
            width = pieceSize(PieceKind.Basket).first,
            height = pieceSize(PieceKind.Basket).second,
        )
        _uiState.update {
            it.copy(
                status = GameStatus.Playing,
                score = 0,
                combo = 0,
                multiplier = 1,
                difficulty = 1f,
                stability = 1f,
                activeSnackSet = snackSet,
                continueUsed = false,
                newHighScore = false,
                stack = listOf(base),
                activePiece = createActivePiece(base.top, snackSet),
                placementMessage = null,
                celebration = null,
            )
        }
    }

    fun onFrame(deltaSeconds: Float) {
        val state = _uiState.value
        if (state.status != GameStatus.Playing) return
        val active = state.activePiece ?: return
        val safeDelta = deltaSeconds.coerceIn(0f, 0.034f)

        when (active.motion) {
            PieceMotion.Moving -> moveHorizontally(active, safeDelta, state.difficulty)
            PieceMotion.Falling -> moveDown(active, safeDelta)
        }
    }

    fun drop(towerCenterX: Float) {
        val state = _uiState.value
        val active = state.activePiece ?: return
        if (state.status != GameStatus.Playing || active.motion != PieceMotion.Moving) return
        _uiState.update {
            it.copy(
                activePiece = active.copy(
                    piece = active.piece.copy(
                        centerX = towerCenterX,
                    ),
                    motion = PieceMotion.Falling,
                )
            )
        }
        _effects.tryEmit(GameEffect.Drop)
    }

    fun pause() {
        _uiState.update {
            if (it.status == GameStatus.Playing) it.copy(status = GameStatus.Paused) else it
        }
    }

    fun resume() {
        _uiState.update {
            if (it.status == GameStatus.Paused) it.copy(status = GameStatus.Playing) else it
        }
    }

    fun exitGame() {
        resumeAfterLifecycle = false
        messageJob?.cancel()
        _uiState.update {
            it.copy(
                status = GameStatus.Ready,
                score = 0,
                combo = 0,
                multiplier = 1,
                difficulty = 1f,
                stability = 1f,
                newHighScore = false,
                stack = emptyList(),
                activePiece = null,
                placementMessage = null,
                celebration = null,
            )
        }
    }

    fun onAppBackgrounded() {
        resumeAfterLifecycle = _uiState.value.status == GameStatus.Playing
        if (resumeAfterLifecycle) pause()
    }

    fun onAppForegrounded() {
        if (resumeAfterLifecycle) {
            resumeAfterLifecycle = false
            resume()
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { it.copy(soundEnabled = enabled) }
        viewModelScope.launch { preferencesStore.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        viewModelScope.launch { preferencesStore.setVibrationEnabled(enabled) }
    }

    fun selectSnackSet(snackSet: SnackSet) {
        val remainingUses = _uiState.value.snackSetUses[snackSet] ?: 0
        if (!canPlaySnackSet(snackSet, remainingUses)) return
        _uiState.update { it.copy(selectedSnackSet = snackSet) }
        viewModelScope.launch { preferencesStore.selectSnackSet(snackSet) }
    }

    fun unlockSnackSet(snackSet: SnackSet) {
        if (snackSet.isFree) return
        _uiState.update {
            it.copy(
                selectedSnackSet = snackSet,
                snackSetUses = it.snackSetUses + (snackSet to REWARDED_SET_RUNS),
            )
        }
        viewModelScope.launch { preferencesStore.unlockSnackSet(snackSet) }
    }

    fun continueGame() {
        val state = _uiState.value
        val towerTop = state.stack.lastOrNull()?.top ?: return
        if (state.status != GameStatus.GameOver || state.continueUsed) return
        _uiState.update {
            it.copy(
                status = GameStatus.Playing,
                combo = 0,
                multiplier = 1,
                continueUsed = true,
                activePiece = createActivePiece(towerTop, state.activeSnackSet),
                placementMessage = null,
                celebration = null,
            )
        }
    }

    private fun moveHorizontally(active: ActivePiece, delta: Float, difficulty: Float) {
        val speed = BASE_HORIZONTAL_SPEED * difficulty
        val halfWidth = active.piece.width / 2f
        var centerX = active.piece.centerX + active.direction * speed * delta
        var direction = active.direction
        if (centerX <= halfWidth) {
            centerX = halfWidth
            direction = 1f
        } else if (centerX >= 1f - halfWidth) {
            centerX = 1f - halfWidth
            direction = -1f
        }
        _uiState.update {
            it.copy(activePiece = active.copy(
                piece = active.piece.copy(centerX = centerX),
                direction = direction,
            ))
        }
    }

    private fun moveDown(active: ActivePiece, delta: Float) {
        val support = _uiState.value.stack.lastOrNull() ?: return
        val nextBottom = active.piece.bottom - DROP_SPEED * delta
        if (nextBottom <= support.top) {
            land(active.piece.copy(bottom = support.top))
        } else {
            _uiState.update {
                it.copy(activePiece = active.copy(piece = active.piece.copy(bottom = nextBottom)))
            }
        }
    }

    private fun land(dropped: GamePiece) {
        val state = _uiState.value
        val result = resolvePlacement(dropped, state.stack, state.combo)
        if (result == null) {
            finishGame()
            return
        }

        val score = state.score + result.points
        val highScore = max(state.highScore, score)
        val difficulty = difficultyForScore(score)
        val speedIncreased = visibleSpeedStep(difficulty) > visibleSpeedStep(state.difficulty)
        val celebration = if (result.perfect || speedIncreased) {
            GameCelebration(
                id = nextCelebrationId++,
                comboMultiplier = result.multiplier.takeIf { result.perfect },
                speed = difficulty.takeIf { speedIncreased },
            )
        } else null
        _uiState.update {
            it.copy(
                status = GameStatus.Playing,
                score = score,
                highScore = highScore,
                combo = result.combo,
                multiplier = result.multiplier,
                difficulty = difficulty,
                stability = result.stability,
                newHighScore = state.newHighScore || highScore > state.highScore,
                stack = state.stack + result.landedPiece,
                activePiece = createActivePiece(result.landedPiece.top, state.activeSnackSet),
                placementMessage = if (result.stability < 0.35f) {
                    PlacementMessage.Unstable
                } else null,
                celebration = celebration ?: state.celebration,
            )
        }

        if (highScore > state.highScore) {
            viewModelScope.launch { preferencesStore.setHighScore(highScore) }
        }
        if (result.perfect) _effects.tryEmit(GameEffect.Perfect)
        clearFeedbackLater()
    }

    private fun finishGame() {
        _uiState.update {
            it.copy(
                status = GameStatus.GameOver,
                activePiece = null,
            )
        }
        _effects.tryEmit(GameEffect.GameOver)
    }

    private fun clearFeedbackLater() {
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            delay(1_400)
            _uiState.update { it.copy(placementMessage = null, celebration = null) }
        }
    }

    private fun createActivePiece(towerTop: Float, snackSet: SnackSet): ActivePiece {
        val kinds = snackSet.pieces
        val kind = kinds[(nextPieceId - 1) % kinds.size]
        val id = nextPieceId++
        val (width, height) = pieceSize(kind)
        val direction = if (id % 2 == 1) 1f else -1f
        return ActivePiece(
            piece = GamePiece(
                id = id,
                kind = kind,
                centerX = if (direction > 0) width / 2f else 1f - width / 2f,
                bottom = spawnBottomFor(towerTop),
                width = width,
                height = height,
            ),
            direction = direction,
        )
    }

}

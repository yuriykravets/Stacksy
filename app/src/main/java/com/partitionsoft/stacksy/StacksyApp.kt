package com.partitionsoft.stacksy

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.partitionsoft.stacksy.core.common.SoundPlayer
import com.partitionsoft.stacksy.core.preferences.PreferencesStore
import com.partitionsoft.stacksy.game.domain.GameEffect
import com.partitionsoft.stacksy.game.domain.GameUiState
import com.partitionsoft.stacksy.game.presentation.GameScreen
import com.partitionsoft.stacksy.game.presentation.GameViewModel
import com.partitionsoft.stacksy.home.HomeScreen

private enum class AppScreen { Home, Game }

@Composable
fun StacksyApp(preferencesStore: PreferencesStore) {
    val gameViewModel: GameViewModel = viewModel { GameViewModel(preferencesStore) }
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val latestState by rememberUpdatedState(uiState)
    val soundPlayer = remember { SoundPlayer() }
    val haptics = LocalHapticFeedback.current

    DisposableEffect(soundPlayer) {
        onDispose(soundPlayer::close)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        gameViewModel.onAppBackgrounded()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        gameViewModel.onAppForegrounded()
    }
    LaunchedEffect(gameViewModel, soundPlayer) {
        gameViewModel.effects.collect { effect ->
            val state = latestState
            if (state.soundEnabled) soundPlayer.play(effect)
            if (state.vibrationEnabled) {
                val type = when (effect) {
                    GameEffect.Drop -> HapticFeedbackType.TextHandleMove
                    GameEffect.Perfect, GameEffect.GameOver -> HapticFeedbackType.LongPress
                }
                haptics.performHapticFeedback(type)
            }
        }
    }

    StacksyContent(
        uiState = uiState,
        onPlay = gameViewModel::startGame,
        onFrame = gameViewModel::onFrame,
        onDrop = gameViewModel::drop,
        onPause = gameViewModel::pause,
        onResume = gameViewModel::resume,
        onRestart = gameViewModel::startGame,
        onExitGame = gameViewModel::exitGame,
        onSoundChanged = gameViewModel::setSoundEnabled,
        onVibrationChanged = gameViewModel::setVibrationEnabled,
    )
}

@Composable
fun StacksyContent(
    uiState: GameUiState,
    onPlay: () -> Unit,
    onFrame: (Float) -> Unit,
    onDrop: (Float) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExitGame: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }

    fun goHome() {
        onExitGame()
        screen = AppScreen.Home
    }

    BackHandler(enabled = screen == AppScreen.Game, onBack = ::goHome)

    when (screen) {
        AppScreen.Home -> HomeScreen(
            highScore = uiState.highScore,
            soundEnabled = uiState.soundEnabled,
            vibrationEnabled = uiState.vibrationEnabled,
            onPlay = {
                onPlay()
                screen = AppScreen.Game
            },
            onSoundChanged = onSoundChanged,
            onVibrationChanged = onVibrationChanged,
        )
        AppScreen.Game -> GameScreen(
            uiState = uiState,
            onFrame = onFrame,
            onDrop = onDrop,
            onPause = onPause,
            onResume = onResume,
            onRestart = onRestart,
            onHome = ::goHome,
        )
    }
}

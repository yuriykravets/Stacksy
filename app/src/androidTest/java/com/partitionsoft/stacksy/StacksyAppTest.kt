package com.partitionsoft.stacksy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.partitionsoft.stacksy.core.design.StacksyTheme
import com.partitionsoft.stacksy.game.domain.GameStatus
import com.partitionsoft.stacksy.game.domain.GameUiState
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class StacksyAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playMovesFromHomeToGame() {
        var state by mutableStateOf(GameUiState())
        composeRule.setContent {
            StacksyTheme {
                StacksyContent(
                    uiState = state,
                    onPlay = { state = state.copy(status = GameStatus.Ready) },
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    onExitGame = {},
                    onSoundChanged = {},
                    onVibrationChanged = {},
                )
            }
        }

        composeRule.onNodeWithTag("play_button").performClick()

        composeRule.onNodeWithTag("game_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("game_board").assertIsDisplayed()
    }

    @Test
    fun settingsToggleChangesSoundPreference() {
        var state by mutableStateOf(GameUiState(soundEnabled = true))
        composeRule.setContent {
            StacksyTheme {
                StacksyContent(
                    uiState = state,
                    onPlay = {},
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    onExitGame = {},
                    onSoundChanged = { state = state.copy(soundEnabled = it) },
                    onVibrationChanged = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("sound_switch").performClick()

        composeRule.runOnIdle { assertFalse(state.soundEnabled) }
    }
}

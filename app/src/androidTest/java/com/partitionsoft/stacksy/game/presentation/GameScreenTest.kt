package com.partitionsoft.stacksy.game.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.partitionsoft.stacksy.core.design.StacksyTheme
import com.partitionsoft.stacksy.game.domain.GameCelebration
import com.partitionsoft.stacksy.game.domain.GameStatus
import com.partitionsoft.stacksy.game.domain.GameUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pausedGameCanResume() {
        var state by mutableStateOf(GameUiState(status = GameStatus.Paused))
        composeRule.setContent {
            StacksyTheme {
                GameScreen(
                    uiState = state,
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = { state = state.copy(status = GameStatus.Ready) },
                    onRestart = {},
                    onHome = {},
                )
            }
        }

        composeRule.onNodeWithTag("pause_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("resume_button").performClick()

        composeRule.onNodeWithTag("game_board").assertIsDisplayed()
    }

    @Test
    fun gameOverSupportsOneTapRestart() {
        var restarted = false
        composeRule.setContent {
            StacksyTheme {
                GameScreen(
                    uiState = GameUiState(
                        status = GameStatus.GameOver,
                        score = 240,
                        highScore = 240,
                    ),
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = { restarted = true },
                    onHome = {},
                )
            }
        }

        composeRule.onNodeWithTag("game_over_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("restart_button").performClick()

        composeRule.runOnIdle { assertTrue(restarted) }
    }

    @Test
    fun comboAndSpeedCelebrationIsVisible() {
        composeRule.setContent {
            StacksyTheme {
                GameScreen(
                    uiState = GameUiState(
                        status = GameStatus.Ready,
                        celebration = GameCelebration(
                            id = 1,
                            comboMultiplier = 3,
                            speed = 1.8f,
                        ),
                    ),
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    onHome = {},
                )
            }
        }

        composeRule.onNodeWithTag("celebration_banner").assertIsDisplayed()
    }

    @Test
    fun gameOverOffersOneRewardedContinue() {
        var continued = false
        composeRule.setContent {
            StacksyTheme {
                GameScreen(
                    uiState = GameUiState(status = GameStatus.GameOver),
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    onContinue = { continued = true },
                    rewardedReady = true,
                    onHome = {},
                )
            }
        }

        composeRule.onNodeWithTag("continue_button").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(continued) }
    }

    @Test
    fun continuedGameDoesNotOfferAnotherContinue() {
        composeRule.setContent {
            StacksyTheme {
                GameScreen(
                    uiState = GameUiState(
                        status = GameStatus.GameOver,
                        continueUsed = true,
                    ),
                    onFrame = {},
                    onDrop = { _ -> },
                    onPause = {},
                    onResume = {},
                    onRestart = {},
                    rewardedReady = true,
                    onHome = {},
                )
            }
        }

        composeRule.onAllNodesWithTag("continue_button").assertCountEquals(0)
    }
}

package com.partitionsoft.stacksy.game.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.partitionsoft.stacksy.R
import com.partitionsoft.stacksy.game.domain.GameCelebration
import com.partitionsoft.stacksy.game.domain.GameStatus
import com.partitionsoft.stacksy.game.domain.GameUiState
import com.partitionsoft.stacksy.game.domain.PlacementMessage

@Composable
fun GameScreen(
    uiState: GameUiState,
    onFrame: (Float) -> Unit,
    onDrop: (Float) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit = {},
    rewardedReady: Boolean = false,
    rewardedLoading: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .testTag("game_screen"),
    ) {
        GameBoard(
            uiState = uiState,
            onFrame = onFrame,
            onDrop = onDrop,
            modifier = Modifier.fillMaxSize(),
        )

        GameHud(
            uiState = uiState,
            onPause = onPause,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp),
        )

        AnimatedContent(
            targetState = uiState.celebration,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            transitionSpec = {
                (fadeIn(tween(180)) +
                    scaleIn(tween(360), initialScale = 0.55f) +
                    slideInVertically(tween(360)) { -it }) togetherWith
                    (fadeOut(tween(160)) +
                        scaleOut(tween(180), targetScale = 0.8f) +
                        slideOutVertically(tween(180)) { -it / 2 })
            },
            label = "game_celebration",
        ) { celebration ->
            if (celebration != null) CelebrationBanner(celebration)
        }

        if (uiState.status == GameStatus.Playing && uiState.celebration == null) {
            Text(
                text = stringResource(R.string.tap_to_drop),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
        }

        when (uiState.status) {
            GameStatus.Paused -> PauseOverlay(onResume = onResume, onHome = onHome)
            GameStatus.GameOver -> GameOverOverlay(
                score = uiState.score,
                highScore = uiState.highScore,
                isNewHighScore = uiState.newHighScore,
                continueUsed = uiState.continueUsed,
                rewardedReady = rewardedReady,
                rewardedLoading = rewardedLoading,
                onContinue = onContinue,
                onRestart = onRestart,
                onHome = onHome,
            )
            else -> Unit
        }
    }
}

@Composable
private fun GameHud(
    uiState: GameUiState,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.score_value, uiState.score),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("score"),
                )
                Text(
                    text = stringResource(R.string.high_score_value, uiState.highScore),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                AnimatedContent(
                    targetState = uiState.multiplier,
                    modifier = Modifier.testTag("combo"),
                    label = "combo_value",
                ) { multiplier ->
                    Text(
                        text = stringResource(R.string.combo_value, multiplier),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                AnimatedContent(
                    targetState = uiState.difficulty,
                    label = "speed_value",
                ) { difficulty ->
                    Text(
                        text = stringResource(R.string.difficulty_value, difficulty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            OutlinedButton(
                onClick = onPause,
                enabled = uiState.status == GameStatus.Playing,
                modifier = Modifier.testTag("pause_button"),
            ) {
                Text(stringResource(R.string.pause))
            }
        }
    }
}

@Composable
private fun CelebrationBanner(celebration: GameCelebration) {
    Box(
        modifier = Modifier
            .widthIn(max = 360.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                    )
                ),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("celebration_banner"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            celebration.comboMultiplier?.let { multiplier ->
                Text(
                    text = stringResource(R.string.combo_celebration, multiplier),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
            celebration.speed?.let { speed ->
                Text(
                    text = stringResource(R.string.speed_up, speed),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onHome: () -> Unit) {
    OverlayCard(testTag = "pause_overlay") {
        Text(
            text = stringResource(R.string.game_paused),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_button"),
        ) {
            Text(stringResource(R.string.resume))
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home))
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    isNewHighScore: Boolean,
    continueUsed: Boolean,
    rewardedReady: Boolean,
    rewardedLoading: Boolean,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
) {
    OverlayCard(testTag = "game_over_overlay") {
        Text(
            text = stringResource(R.string.game_over),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        if (isNewHighScore) {
            Text(
                text = stringResource(R.string.new_high_score),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.score_value, score), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.high_score_value, highScore))
        Spacer(Modifier.height(20.dp))
        if (!continueUsed) {
            Text(
                text = stringResource(R.string.continue_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                enabled = rewardedReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("continue_button"),
            ) {
                Text(
                    stringResource(
                        when {
                            rewardedReady -> R.string.watch_ad_continue
                            rewardedLoading -> R.string.ad_loading
                            else -> R.string.ad_unavailable
                        }
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("restart_button"),
        ) {
            Text(stringResource(R.string.restart))
        }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home))
        }
    }
}

@Composable
private fun OverlayCard(
    testTag: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f))
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

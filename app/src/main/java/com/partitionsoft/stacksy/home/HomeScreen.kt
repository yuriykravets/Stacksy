package com.partitionsoft.stacksy.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.partitionsoft.stacksy.R
import com.partitionsoft.stacksy.collection.domain.SnackSet
import com.partitionsoft.stacksy.collection.presentation.nameRes
import com.partitionsoft.stacksy.settings.SettingsSheet

@Composable
fun HomeScreen(
    highScore: Int,
    selectedSnackSet: SnackSet,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onPlay: () -> Unit,
    onOpenCollection: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    privacyOptionsRequired: Boolean,
    onPrivacyOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val background = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant,
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(36.dp))
            SnackTowerPreview(selectedSnackSet)
            Spacer(Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.high_score),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = highScore.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("home_high_score"),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("play_button"),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            ) {
                Text(stringResource(R.string.play), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenCollection,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("collection_button"),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    stringResource(
                        R.string.current_set,
                        stringResource(selectedSnackSet.nameRes()),
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_button"),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(stringResource(R.string.settings))
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            onSoundChanged = onSoundChanged,
            onVibrationChanged = onVibrationChanged,
            privacyOptionsRequired = privacyOptionsRequired,
            onPrivacyOptions = onPrivacyOptions,
            onDismiss = { showSettings = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SnackTowerPreview(snackSet: SnackSet = SnackSet.Classic) {
    val description = stringResource(R.string.tower_preview)
    Column(
        modifier = Modifier
            .semantics { contentDescription = description }
            .testTag("tower_preview"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-8).dp),
    ) {
        PreviewPiece(snackSet.pieces[2].emoji, 64)
        PreviewPiece(snackSet.pieces[1].emoji, 78)
        PreviewPiece(snackSet.pieces[0].emoji, 92)
        PreviewPiece("🧺", 112)
    }
}

@Composable
private fun PreviewPiece(symbol: String, width: Int) {
    Box(
        modifier = Modifier
            .width(width.dp)
            .height(52.dp)
            .shadow(5.dp, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.headlineLarge)
    }
}

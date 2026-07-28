package com.partitionsoft.stacksy.collection.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.partitionsoft.stacksy.R
import com.partitionsoft.stacksy.collection.domain.SnackSet

@Composable
fun SnackCollectionScreen(
    selectedSnackSet: SnackSet,
    remainingUses: Map<SnackSet, Int>,
    rewardedReady: Boolean,
    rewardedLoading: Boolean,
    onSelect: (SnackSet) -> Unit,
    onUnlock: (SnackSet) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("snack_collection_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.snack_collection),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.choose_snack_set),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SnackSet.entries.forEach { snackSet ->
            SnackSetCard(
                snackSet = snackSet,
                selected = selectedSnackSet == snackSet,
                remainingUses = remainingUses[snackSet] ?: 0,
                rewardedReady = rewardedReady,
                rewardedLoading = rewardedLoading,
                onSelect = { onSelect(snackSet) },
                onUnlock = { onUnlock(snackSet) },
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("collection_back"),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun SnackSetCard(
    snackSet: SnackSet,
    selected: Boolean,
    remainingUses: Int,
    rewardedReady: Boolean,
    rewardedLoading: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit,
) {
    val available = snackSet.isFree || remainingUses > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("snack_set_${snackSet.storageId}"),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(snackSet.nameRes()),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = when {
                        snackSet.isFree -> stringResource(R.string.free)
                        else -> pluralStringResource(
                            R.plurals.runs_left,
                            remainingUses,
                            remainingUses,
                        )
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                text = snackSet.pieces.joinToString(separator = " ") { it.emoji },
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(snackSet.descriptionRes()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )

            if (available) {
                Button(
                    onClick = onSelect,
                    enabled = !selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_${snackSet.storageId}"),
                ) {
                    Text(
                        stringResource(
                            if (selected) R.string.snack_set_selected else R.string.select_set
                        )
                    )
                }
            } else {
                Button(
                    onClick = onUnlock,
                    enabled = rewardedReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_${snackSet.storageId}"),
                ) {
                    Text(
                        stringResource(
                            when {
                                rewardedReady -> R.string.watch_ad_unlock
                                rewardedLoading -> R.string.ad_loading
                                else -> R.string.ad_unavailable
                            }
                        )
                    )
                }
            }
        }
    }
}

@StringRes
internal fun SnackSet.nameRes(): Int = when (this) {
    SnackSet.Classic -> R.string.classic_snacks
    SnackSet.SushiParty -> R.string.sushi_party
    SnackSet.SweetDreams -> R.string.sweet_dreams
    SnackSet.TropicalMix -> R.string.tropical_mix
}

@StringRes
private fun SnackSet.descriptionRes(): Int = when (this) {
    SnackSet.Classic -> R.string.classic_snacks_description
    SnackSet.SushiParty -> R.string.sushi_party_description
    SnackSet.SweetDreams -> R.string.sweet_dreams_description
    SnackSet.TropicalMix -> R.string.tropical_mix_description
}

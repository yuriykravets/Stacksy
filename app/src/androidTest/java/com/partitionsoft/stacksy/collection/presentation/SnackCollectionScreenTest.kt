package com.partitionsoft.stacksy.collection.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.partitionsoft.stacksy.collection.domain.SnackSet
import com.partitionsoft.stacksy.core.design.StacksyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SnackCollectionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rewardedSetUnlockRequiresAnAvailableAd() {
        var unlocked: SnackSet? = null
        composeRule.setContent {
            StacksyTheme {
                SnackCollectionScreen(
                    selectedSnackSet = SnackSet.Classic,
                    remainingUses = emptyMap(),
                    rewardedReady = true,
                    rewardedLoading = false,
                    onSelect = {},
                    onUnlock = { unlocked = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("unlock_sushi_party")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(SnackSet.SushiParty, unlocked) }
    }

    @Test
    fun unlockedSetCanBeSelectedWithoutAnotherAd() {
        var selected: SnackSet? = null
        composeRule.setContent {
            StacksyTheme {
                SnackCollectionScreen(
                    selectedSnackSet = SnackSet.Classic,
                    remainingUses = mapOf(SnackSet.SweetDreams to 2),
                    rewardedReady = false,
                    rewardedLoading = false,
                    onSelect = { selected = it },
                    onUnlock = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("select_sweet_dreams")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(SnackSet.SweetDreams, selected) }
    }
}

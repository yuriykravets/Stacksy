package com.partitionsoft.stacksy.collection.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackSetTest {
    @Test
    fun classicSetIsAlwaysPlayable() {
        assertTrue(canPlaySnackSet(SnackSet.Classic, remainingUses = 0))
        assertEquals(0, remainingUsesAfterStarting(SnackSet.Classic, remainingUses = 0))
    }

    @Test
    fun rewardedSetRequiresAndConsumesOneUse() {
        assertFalse(canPlaySnackSet(SnackSet.SushiParty, remainingUses = 0))
        assertTrue(canPlaySnackSet(SnackSet.SushiParty, remainingUses = 3))
        assertEquals(
            2,
            remainingUsesAfterStarting(SnackSet.SushiParty, remainingUses = 3),
        )
    }

    @Test
    fun rewardedUsesNeverBecomeNegative() {
        assertEquals(
            0,
            remainingUsesAfterStarting(SnackSet.SweetDreams, remainingUses = 0),
        )
    }

    @Test
    fun storageIdsRestoreKnownSetsAndFallBackToClassic() {
        assertEquals(SnackSet.TropicalMix, SnackSet.fromStorageId("tropical_mix"))
        assertEquals(SnackSet.Classic, SnackSet.fromStorageId("unknown"))
        assertEquals(SnackSet.Classic, SnackSet.fromStorageId(null))
    }
}

package com.partitionsoft.stacksy.collection.domain

import com.partitionsoft.stacksy.game.domain.PieceKind

const val REWARDED_SET_RUNS = 3

enum class SnackSet(
    val storageId: String,
    val pieces: List<PieceKind>,
) {
    Classic(
        storageId = "classic",
        pieces = listOf(
            PieceKind.Donut,
            PieceKind.Burger,
            PieceKind.Cheese,
            PieceKind.Cupcake,
            PieceKind.Pizza,
            PieceKind.Fries,
            PieceKind.Cookie,
            PieceKind.Watermelon,
            PieceKind.Present,
        ),
    ),
    SushiParty(
        storageId = "sushi_party",
        pieces = listOf(
            PieceKind.Sushi,
            PieceKind.RiceBall,
            PieceKind.Shrimp,
            PieceKind.Ramen,
            PieceKind.Bento,
        ),
    ),
    SweetDreams(
        storageId = "sweet_dreams",
        pieces = listOf(
            PieceKind.Cake,
            PieceKind.Candy,
            PieceKind.IceCream,
            PieceKind.Chocolate,
            PieceKind.Lollipop,
        ),
    ),
    TropicalMix(
        storageId = "tropical_mix",
        pieces = listOf(
            PieceKind.Pineapple,
            PieceKind.Mango,
            PieceKind.Coconut,
            PieceKind.Banana,
            PieceKind.Kiwi,
        ),
    );

    val isFree: Boolean get() = this == Classic

    companion object {
        fun fromStorageId(value: String?): SnackSet =
            entries.firstOrNull { it.storageId == value } ?: Classic
    }
}

fun canPlaySnackSet(snackSet: SnackSet, remainingUses: Int): Boolean =
    snackSet.isFree || remainingUses > 0

fun remainingUsesAfterStarting(snackSet: SnackSet, remainingUses: Int): Int =
    if (snackSet.isFree) remainingUses else (remainingUses - 1).coerceAtLeast(0)

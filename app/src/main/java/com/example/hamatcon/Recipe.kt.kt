package com.example.hamatcon

data class Recipe(
    val name: String = "",
    val matchPercent: Int = 0,
    val difficulty: String = "",
    val cookTime: String = "",
    val thumbnailResId: Int = 0,
    val cuisine: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    val id: String = "",
    val favoritesCount: Int = 0,
    val imageUrl: String = "",
    val ratingSum: Int = 0,
    val ratingCount: Int = 0
) {
    fun averageRating(): Float =
        when {
            ratingCount > 0 -> (ratingSum.toFloat() / 2f) / ratingCount
            else -> 0f
        }
}
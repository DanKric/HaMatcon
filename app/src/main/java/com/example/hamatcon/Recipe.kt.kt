package com.example.hamatcon

data class Recipe(
    val name: String,
    val matchPercent: Int,
    val difficulty: String,
    val cookTime: String,
    val thumbnailResId: Int,
    val cuisine: String,
    val ingredients: List<String>,
    val instructions: String,
    val ratings: List<Int>
) {
    fun averageRating(): Float {
        return if (ratings.isNotEmpty()) {
            ratings.average().toFloat()
        } else {
            0f
        }
    }
}

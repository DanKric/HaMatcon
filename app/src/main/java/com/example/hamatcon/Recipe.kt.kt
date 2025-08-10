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
    val ratings: List<Int> = emptyList(),
    val id: String = ""           // ✅ add this
)
{
    fun averageRating(): Float {
        return if (ratings.isNotEmpty()) {
            ratings.average().toFloat()
        } else {
            0f
        }
    }
}

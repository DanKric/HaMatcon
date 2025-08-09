package com.example.hamatcon.data.remote.mealdb

import com.google.firebase.Timestamp

data class RecipeDoc(
    val name: String = "",
    val cuisine: String = "",
    val cookTime: String = "",          // MealDB doesn’t provide — leave empty
    val difficulty: String = "",        // not provided — leave empty
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    val imageUrl: String? = null,
    val ownerUid: String = "seed",
    val favoritesCount: Int = 0,
    val ratingSum: Int = 0,
    val ratingCount: Int = 0,
    val ratingAvg: Double = 0.0,
    val createdAt: Timestamp = Timestamp.now(),
    val source: String = "TheMealDB",
    val externalId: String? = null
)

fun MealDetail.toRecipeDoc(): RecipeDoc {
    return RecipeDoc(
        name = strMeal.orEmpty(),
        cuisine = strArea.orEmpty(),
        cookTime = "",
        difficulty = "",
        ingredients = extractIngredients(this),
        instructions = strInstructions.orEmpty(),
        imageUrl = strMealThumb,
        ownerUid = "seed",
        favoritesCount = 0,
        ratingSum = 0,
        ratingCount = 0,
        ratingAvg = 0.0,
        createdAt = Timestamp.now(),
        source = "TheMealDB",
        externalId = idMeal
    )
}

private fun extractIngredients(m: MealDetail): List<String> {
    // Join "measure ingredient" when available; keep simple if measure empty.
    fun pair(i: String?, q: String?): String? {
        val ing = i?.trim().orEmpty()
        val qty = q?.trim().orEmpty()
        if (ing.isBlank()) return null
        return if (qty.isNotBlank()) "$qty $ing" else ing
    }

    return listOfNotNull(
        pair(m.strIngredient1, m.strMeasure1),
        pair(m.strIngredient2, m.strMeasure2),
        pair(m.strIngredient3, m.strMeasure3),
        pair(m.strIngredient4, m.strMeasure4),
        pair(m.strIngredient5, m.strMeasure5),
        pair(m.strIngredient6, m.strMeasure6),
        pair(m.strIngredient7, m.strMeasure7),
        pair(m.strIngredient8, m.strMeasure8),
        pair(m.strIngredient9, m.strMeasure9),
        pair(m.strIngredient10, m.strMeasure10),
        pair(m.strIngredient11, m.strMeasure11),
        pair(m.strIngredient12, m.strMeasure12),
        pair(m.strIngredient13, m.strMeasure13),
        pair(m.strIngredient14, m.strMeasure14),
        pair(m.strIngredient15, m.strMeasure15),
        pair(m.strIngredient16, m.strMeasure16),
        pair(m.strIngredient17, m.strMeasure17),
        pair(m.strIngredient18, m.strMeasure18),
        pair(m.strIngredient19, m.strMeasure19),
        pair(m.strIngredient20, m.strMeasure20),
    ).filter { it.isNotBlank() }
}

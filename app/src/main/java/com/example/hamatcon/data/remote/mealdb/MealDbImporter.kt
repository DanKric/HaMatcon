package com.example.hamatcon.data.remote.mealdb

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MealDbImporter(
    private val api: MealDbService,
    private val db: FirebaseFirestore
) {
    private val recipesCol = db.collection("Recipes")

    // Pulls N recipes per category, looks up full details, and upserts.
    suspend fun importCategories(
        categories: List<String>,
        maxPerCategory: Int = 30 // tweak to hit ~50–100 total
    ): Int = withContext(Dispatchers.IO) {
        var imported = 0

        for (cat in categories) {
            val filterRes = api.filterByCategory(cat).meals.orEmpty()
            val slice = filterRes.take(maxPerCategory)

            for (m in slice) {
                val details = api.lookupById(m.idMeal).meals?.firstOrNull() ?: continue
                val doc = details.toRecipeDoc()
                val docId = "mealdb_${details.idMeal}"

                // Upsert (merge) to avoid duplicates
                recipesCol.document(docId).set(doc)
                    .addOnFailureListener { /* log if you want */ }

                imported++
            }
        }
        imported
    }
}

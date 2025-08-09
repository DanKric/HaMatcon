package com.example.hamatcon.logic

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

object RecipeAutofill {

    // --- Public: run once to backfill seeded recipes ---
    // ownerUidFilter = "seed" updates only the imported recipes. Pass null to update all.
    suspend fun backfillSeededTimeAndDifficulty(
        db: FirebaseFirestore,
        ownerUidFilter: String? = "seed"
    ): Int = withContext(Dispatchers.IO) {
        val query = db.collection("Recipes").let { col ->
            if (ownerUidFilter != null) col.whereEqualTo("ownerUid", ownerUidFilter) else col
        }
        val snap = query.get().await()

        var batch = db.batch()
        var ops = 0

        for (doc in snap.documents) {
            val data = doc.data ?: continue
            val name = (data["name"] as? String).orEmpty()
            val instructions = (data["instructions"] as? String).orEmpty()
            val ingredients = (data["ingredients"] as? List<*>)?.map { it.toString() } ?: emptyList<String>()

            val steps = countSteps(instructions)
            val cookMin = estimateCookTimeMinutes(instructions, ingredients)
            val cookTimeStr = cookMin?.let { "$it min" } ?: ""
            val difficulty = estimateDifficulty(
                ingredientCount = ingredients.size,
                steps = steps,
                cookMin = cookMin,
                name = name,
                ingredients = ingredients
            )

            val updates = hashMapOf<String, Any>(
                "cookTime" to cookTimeStr,
                "difficulty" to difficulty
            )
            batch.update(doc.reference, updates)
            ops++

            // Firestore batch safety
            if (ops % 450 == 0) {
                batch.commit().await()
                batch = db.batch()
            }
        }

        if (ops % 450 != 0) batch.commit().await()
        ops
    }

    // --- Heuristics ---

    // Parse explicit times in instructions and then apply ingredient/technique guards
    fun estimateCookTimeMinutes(instructions: String, ingredients: List<String>): Int? {
        val text = instructions.lowercase()

        // 1) Parse times mentioned in text
        var total = 0
        var found = false
        // e.g., 1h 30m
        Regex("""(\d+)\s*h(?:ours?)?\s*(\d+)\s*m""").findAll(text).forEach {
            total += it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
            found = true
        }
        // e.g., 2h
        Regex("""(\d+)\s*h(?:ours?)?""").findAll(text).forEach {
            total += it.groupValues[1].toInt() * 60
            found = true
        }
        // e.g., 45 m / 45 min / 45 minutes
        Regex("""(\d+)\s*m(?:in(?:ute)?s?)?""").findAll(text).forEach {
            total += it.groupValues[1].toInt()
            found = true
        }

        // If no explicit times, start with a technique baseline
        var minutes: Int = if (found) total else baselineByTechnique(text)

        // 2) Ingredient guards (caps + floors)
        val ing = ingredients.joinToString(" | ").lowercase()

        // Your rule: any beef dish is at least Medium and min 30 minutes
        if (containsAny(ing, "beef", "steak", "brisket", "chuck", "sirloin")) {
            minutes = max(minutes, 30)
        }
        // Common protein floors
        if (containsAny(ing, "lamb", "mutton")) minutes = max(minutes, 45)
        if (containsAny(ing, "pork")) minutes = max(minutes, 35)
        if (containsAny(ing, "chicken thigh", "chicken legs", "whole chicken")) minutes = max(minutes, 45)
        else if (containsAny(ing, "chicken breast", "chicken")) minutes = max(minutes, 25)
        if (containsAny(ing, "turkey")) minutes = max(minutes, 40)
        if (containsAny(ing, "dried beans", "dry beans", "kidney beans (dry)", "chickpeas (dry)")) minutes = max(minutes, 60)
        if (containsAny(ing, "rice", "risotto")) minutes = max(minutes, 18) // simmer time; assumes prep overlaps
        if (containsAny(ing, "pasta", "spaghetti", "penne", "fettuccine")) minutes = max(minutes, 10)

        // 3) Technique floors (braise/stew/roast should not be too short)
        if (containsAny(text, "braise", "stew", "slow cook", "simmer for")) minutes = max(minutes, 60)
        if (containsAny(text, "bake", "roast")) minutes = max(minutes, 30)
        if (containsAny(text, "deep-fry", "deep fry")) minutes = max(minutes, 15)
        if (containsAny(text, "marinate")) minutes = max(minutes, 30) // simple minimum marinade

        // Clean result
        return if (minutes <= 0) null else minutes
    }

    fun countSteps(instructions: String): Int {
        val lines = instructions.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size >= 2) return lines.size
        val sentences = instructions.split(Regex("""(?<=[.!?])\s+"""))
            .map { it.trim() }.filter { it.isNotEmpty() }
        return max(1, sentences.size)
    }

    fun estimateDifficulty(
        ingredientCount: Int,
        steps: Int,
        cookMin: Int?,
        name: String,
        ingredients: List<String>
    ): String {
        val min = cookMin ?: 0
        val text = (name + " " + ingredients.joinToString(" ")).lowercase()

        var score = 0
        // Base on complexity
        if (ingredientCount >= 10) score += 1
        if (ingredientCount >= 15) score += 1
        if (steps >= 8) score += 1
        if (steps >= 12) score += 1
        if (min >= 45) score += 1
        if (min >= 90) score += 1

        // Techniques that usually raise difficulty
        if (containsAny(text, "emulsify", "temper", "caramelize", "poach", "confit", "hollandaise", "souffle", "proof"))
            score += 1

        // Protein‑based difficulty bump
        if (containsAny(text, "beef", "pork", "lamb")) score += 1

        // Map score to labels
        var label = when {
            score <= 1 -> "Easy"
            score <= 3 -> "Medium"
            else -> "Hard"
        }

        // Enforce your business rule: beef is at least Medium
        if (containsAny(text, "beef", "steak", "brisket", "chuck", "sirloin") && label == "Easy") {
            label = "Medium"
        }

        return label
    }

    // --- helpers ---
    private fun containsAny(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it, ignoreCase = true) }

    private fun baselineByTechnique(text: String): Int {
        return when {
            containsAny(text, "braise", "stew", "slow cook") -> 75
            containsAny(text, "bake", "roast") -> 40
            containsAny(text, "grill", "barbecue", "bbq") -> 20
            containsAny(text, "stir-fry", "stir fry", "saute", "sauté") -> 12
            containsAny(text, "boil", "simmer") -> 20
            else -> 15 // generic prep+cook baseline if no hints
        }
    }
}

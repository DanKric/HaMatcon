package com.example.hamatcon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.hamatcon.databinding.ActivityRecipeDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeDetailBinding
    private var recipeId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recipe Details"

        // Get extras
        recipeId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val cuisine = intent.getStringExtra(EXTRA_CUISINE).orEmpty()
        val cookTimeRaw = intent.getStringExtra(EXTRA_COOK_TIME)
        val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY).orEmpty()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL).orEmpty()
        val ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS) ?: arrayListOf()
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS).orEmpty()
        val avgFromList = intent.getFloatExtra(EXTRA_AVG_RATING, 0f)

        // Bind views you actually have in the layout
        binding.textViewRecipeName.text = name
        binding.textViewDifficulty.text = difficulty
        binding.textViewCookTime.text = formatCookTime(cookTimeRaw)
        // If you DO add a TextView with id textViewCuisine, uncomment next line:
        // binding.textViewCuisine.text = cuisine

        binding.imageViewThumbnail.load(imageUrl.ifBlank { null }) {
            placeholder(R.drawable.placeholder)
            error(R.drawable.placeholder)
            crossfade(true)
        }
        binding.textViewIngredients.text = ingredients.joinToString("\n• ", prefix = "• ")
        binding.textViewInstructions.text = instructions

        // Average stars shown here only (not interactive on scroll)
        binding.ratingBar.stepSize = 0.5f
        binding.ratingBar.rating = avgFromList
        refreshAverage() // optional: fetch fresh avg

        // "Rate this recipe" button must exist in your layout as @id/buttonRate
        binding.buttonRate.setOnClickListener {
            if (recipeId.isBlank()) {
                android.widget.Toast.makeText(this, "Missing recipe ID", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showRateDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    // --- Rating flow via dialog ---
    private fun showRateDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_rate_recipe, null)
        val rb = view.findViewById<android.widget.RatingBar>(R.id.ratingInput)

        // Prefill with user's previous rating (support old whole-star 'value' and new half-star 'value2')
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null && recipeId.isNotBlank()) {
            com.google.firebase.ktx.Firebase.firestore
                .collection("Recipes").document(recipeId)
                .collection("ratings").document(uid)
                .get()
                .addOnSuccessListener { snap ->
                    val v2 = snap.getLong("value2")?.toInt()
                    val v = snap.getLong("value")?.toInt()
                    when {
                        v2 != null -> rb.rating = v2 / 2f
                        v != null -> rb.rating = v.toFloat()
                    }
                }
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Rate this recipe")
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Submit") { _, _ ->
                submitRatingHalfStars(rb.rating)  // 0.5-step value
            }
            .show()
    }


    private fun submitRatingHalfStars(rating: Float) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: run {
            android.widget.Toast.makeText(this, "Please sign in to rate", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val value2 = kotlin.math.round(rating * 2f).toInt().coerceIn(1, 10) // 0.5→1 .. 5.0→10

        val db = com.google.firebase.ktx.Firebase.firestore
        val recipeRef = db.collection("Recipes").document(recipeId)
        val userRatingRef = recipeRef.collection("ratings").document(uid)

        db.runTransaction { tx ->
            val snap = tx.get(recipeRef)
            val sum = (snap.getLong("ratingSum") ?: 0L).toInt()      // half-star units
            val cnt = (snap.getLong("ratingCount") ?: 0L).toInt()

            val userSnap = tx.get(userRatingRef)
            if (userSnap.exists()) {
                val old2 = (userSnap.getLong("value2") ?: 0L).toInt()
                val delta = value2 - old2
                tx.update(recipeRef, mapOf("ratingSum" to (sum + delta), "ratingCount" to cnt))
            } else {
                tx.update(recipeRef, mapOf("ratingSum" to (sum + value2), "ratingCount" to (cnt + 1)))
            }

            tx.set(userRatingRef, mapOf(
                "value2" to value2,
                "ts" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge())
            null
        }.addOnSuccessListener {
            android.widget.Toast.makeText(this, "Thanks for rating!", android.widget.Toast.LENGTH_SHORT).show()
            refreshAverage()
        }.addOnFailureListener { e ->
            android.widget.Toast.makeText(this, "Rating failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshAverage() {
        if (recipeId.isBlank()) return
        com.google.firebase.ktx.Firebase.firestore.collection("Recipes").document(recipeId).get()
            .addOnSuccessListener { d ->
                val sum2 = (d.getLong("ratingSum") ?: 0L).toFloat()  // half-star units
                val cnt = (d.getLong("ratingCount") ?: 0L).toFloat()
                val avg = if (cnt > 0f) (sum2 / 2f) / cnt else 0f
                binding.ratingBar.setIsIndicator(true)
                binding.ratingBar.setStepSize(0.5f)
                binding.ratingBar.rating = avg
            }
    }


    private fun formatCookTime(raw: String?): String {
        val m = Regex("(\\d+)").find(raw ?: "")
        val minutes = m?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return raw ?: ""
        return if (minutes >= 60) {
            val h = minutes / 60; val rem = minutes % 60
            if (rem == 0) "${h} hr" else "${h} hr ${rem} min"
        } else "${minutes} min"
    }

    companion object {
        private const val EXTRA_ID = "extra_id"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_CUISINE = "extra_cuisine"
        private const val EXTRA_COOK_TIME = "extra_cook_time"
        private const val EXTRA_DIFFICULTY = "extra_difficulty"
        private const val EXTRA_INGREDIENTS = "extra_ingredients"
        private const val EXTRA_INSTRUCTIONS = "extra_instructions"
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_AVG_RATING = "extra_avg_rating"

        fun start(context: Context, r: Recipe) {
            val i = Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra(EXTRA_ID, r.id)
                putExtra(EXTRA_NAME, r.name)
                putExtra(EXTRA_CUISINE, r.cuisine)
                putExtra(EXTRA_COOK_TIME, r.cookTime)
                putExtra(EXTRA_DIFFICULTY, r.difficulty)
                putStringArrayListExtra(EXTRA_INGREDIENTS, ArrayList(r.ingredients))
                putExtra(EXTRA_INSTRUCTIONS, r.instructions)
                putExtra(EXTRA_IMAGE_URL, r.imageUrl)
                putExtra(EXTRA_AVG_RATING, r.averageRating())
            }
            context.startActivity(i)
        }
    }
}

package com.example.hamatcon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hamatcon.databinding.ActivityRecipeDetailBinding

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Enable up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recipe Details"

        // Get data from Intent extras
        val name = intent.getStringExtra(EXTRA_NAME) ?: ""
        val matchPercent = intent.getIntExtra(EXTRA_MATCH_PERCENT, 0)
        val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY) ?: ""
        val cookTime = intent.getStringExtra(EXTRA_COOK_TIME) ?: ""
        val thumbnailResId = intent.getIntExtra(EXTRA_THUMBNAIL_RES_ID, 0)
        val ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS) ?: arrayListOf()
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""

        val ratings = intent.getIntegerArrayListExtra(EXTRA_RATINGS) ?: arrayListOf()
        val avgRating = if (ratings.isNotEmpty()) {
            ratings.average().toFloat()
        } else {
            0f
        }
        binding.ratingBar.rating = avgRating
        binding.textViewRecipeName.text = name
        binding.textViewMatchPercent.text = "$matchPercent% match"
        binding.textViewDifficulty.text = difficulty
        binding.textViewCookTime.text = cookTime
        binding.imageViewThumbnail.setImageResource(thumbnailResId)
        binding.textViewIngredients.text = ingredients.joinToString(separator = "\n") { "• $it" }
        binding.textViewInstructions.text = instructions

    }

    // Handle up navigation
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_MATCH_PERCENT = "extra_match_percent"
        private const val EXTRA_DIFFICULTY = "extra_difficulty"
        private const val EXTRA_COOK_TIME = "extra_cook_time"
        private const val EXTRA_THUMBNAIL_RES_ID = "extra_thumbnail_res_id"
        private const val EXTRA_INGREDIENTS = "extra_ingredients"
        private const val EXTRA_INSTRUCTIONS = "extra_instructions"
        private const val EXTRA_RATINGS = "extra_ratings"


        // Helper to launch this activity with a Recipe
        fun start(context: Context, recipe: Recipe) {
            val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra(EXTRA_NAME, recipe.name)
                putExtra(EXTRA_MATCH_PERCENT, recipe.matchPercent)
                putExtra(EXTRA_DIFFICULTY, recipe.difficulty)
                putExtra(EXTRA_COOK_TIME, recipe.cookTime)
                putExtra(EXTRA_THUMBNAIL_RES_ID, recipe.thumbnailResId)
                putStringArrayListExtra(EXTRA_INGREDIENTS, ArrayList(recipe.ingredients))
                putIntegerArrayListExtra(EXTRA_RATINGS, ArrayList(recipe.ratings))
                putExtra(EXTRA_INSTRUCTIONS, recipe.instructions)
            }
            context.startActivity(intent)
        }
    }
}
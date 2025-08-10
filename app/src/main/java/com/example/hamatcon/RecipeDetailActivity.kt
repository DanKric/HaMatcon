package com.example.hamatcon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.hamatcon.databinding.ActivityRecipeDetailBinding

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecipeDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Up navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Recipe Details"

        // Get data from Intent extras
        val name = intent.getStringExtra(EXTRA_NAME) ?: ""
        val matchPercent = intent.getIntExtra(EXTRA_MATCH_PERCENT, 0)
        val difficulty = intent.getStringExtra(EXTRA_DIFFICULTY) ?: ""
        val cookTimeRaw = intent.getStringExtra(EXTRA_COOK_TIME)
        val thumbnailResId = intent.getIntExtra(EXTRA_THUMBNAIL_RES_ID, 0)
        val ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS) ?: arrayListOf()
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""
        val ratings = intent.getIntegerArrayListExtra(EXTRA_RATINGS) ?: arrayListOf()
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL) ?: ""      // ✅ new

        // Bind fields
        binding.ratingBar.rating = if (ratings.isNotEmpty()) ratings.average().toFloat() else 0f
        binding.textViewRecipeName.text = name
        binding.textViewMatchPercent.text = "$matchPercent% match"
        binding.textViewDifficulty.text = difficulty
        binding.textViewCookTime.text = formatCookTime(cookTimeRaw)       // ✅ pretty minutes

        // ✅ Load the picture (URL first, then fallback to drawable or placeholder)
        if (imageUrl.isNotBlank()) {
            binding.imageViewThumbnail.load(imageUrl) {
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
                crossfade(true)
            }
        } else if (thumbnailResId != 0) {
            binding.imageViewThumbnail.setImageResource(thumbnailResId)
        } else {
            binding.imageViewThumbnail.setImageResource(R.drawable.placeholder)
        }

        binding.textViewIngredients.text = ingredients.joinToString(separator = "\n") { "• $it" }
        binding.textViewInstructions.text = instructions
    }

    // Handle up navigation
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun formatCookTime(raw: String?): String {
        val m = Regex("(\\d+)").find(raw ?: "")
        val minutes = m?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return raw ?: ""
        return if (minutes >= 60) {
            val h = minutes / 60
            val rem = minutes % 60
            if (rem == 0) "${h} hr" else "${h} hr ${rem} min"
        } else "${minutes} min"
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
        private const val EXTRA_IMAGE_URL = "extra_image_url"            // ✅ new

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
                putExtra(EXTRA_IMAGE_URL, recipe.imageUrl)               // ✅ pass it
            }
            context.startActivity(intent)
        }
    }
}

package com.example.hamatcon

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.hamatcon.databinding.ItemRecipeBinding

class RecipeAdapter(private val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    // ---- Favorites helpers ----
    private var favoriteIds: Set<String> = emptySet()
    fun setFavoriteIds(ids: Set<String>) { favoriteIds = ids; notifyDataSetChanged() }
    var onFavoriteClick: ((recipeId: String, isCurrentlyFav: Boolean) -> Unit)? = null
    // ---------------------------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        // Heart icon (use binding)
        holder.binding.btnFavorite?.let { btn ->
            val isFav = favoriteIds.contains(recipe.id)
            btn.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            btn.setOnClickListener { onFavoriteClick?.invoke(recipe.id, isFav) }
        }

        // Bind the rest
        holder.binding.ratingBar.rating = recipe.averageRating()
        holder.binding.textViewRecipeName.text = recipe.name.ifEmpty { "Unnamed Recipe" }
        holder.binding.textViewMatchPercent.text = "${recipe.matchPercent}% match"
        holder.binding.textViewDifficulty.text = recipe.difficulty
        holder.binding.textViewCookTime.text = recipe.cookTime
        holder.binding.imageViewThumbnail.setImageResource(recipe.thumbnailResId)

        holder.itemView.setOnClickListener {
            RecipeDetailActivity.start(holder.itemView.context, recipe)
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun updateList(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }
}

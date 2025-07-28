package com.example.hamatcon

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hamatcon.databinding.ItemRecipeBinding

class RecipeAdapter(private var recipes: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        val avgRating = recipe.averageRating()

        holder.binding.ratingBar.rating = avgRating
        holder.binding.textViewRecipeName.text = recipe.name
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
        this.recipes = newRecipes
        notifyDataSetChanged()
    }
}

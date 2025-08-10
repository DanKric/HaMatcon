package com.example.hamatcon

import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hamatcon.databinding.ItemRecipeBinding
import coil.load


class RecipeAdapter(private val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    // ---- Favorites helpers ----
    private var favoriteIds: Set<String> = emptySet()
    fun setFavoriteIds(ids: Set<String>) { favoriteIds = ids; notifyDataSetChanged() }

    var onFavoriteClick: ((recipeId: String, isCurrentlyFav: Boolean) -> Unit)? = null
    // ---------------------------

    // 🔽 NEW: control whether favorites count is visible
    private var showFavoritesCount: Boolean = false
    fun setShowFavoritesCount(show: Boolean) {
        showFavoritesCount = show
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        // Heart toggle
        holder.binding.btnFavorite?.let { btn ->
            val isFav = favoriteIds.contains(recipe.id)
            btn.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            btn.setOnClickListener { onFavoriteClick?.invoke(recipe.id, isFav) }
        }

        // Favorites count (shown only if flag enabled)
        holder.binding.textViewFavoritesCount?.apply {
            visibility = if (showFavoritesCount) View.VISIBLE else View.GONE
            text = recipe.favoritesCount.toString()
        }

        // Format cook time (e.g., "90" or "90 min" -> "1 hr 30 min")
        val cookTimeFormatted = run {
            // Extract the first continuous number in the string, if any
            val numMatch = Regex("(\\d+)").find(recipe.cookTime ?: "")
            val minutes = numMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (minutes != null) {
                if (minutes >= 60) {
                    val h = minutes / 60
                    val m = minutes % 60
                    if (m == 0) "${h} hr" else "${h} hr ${m} min"
                } else {
                    "${minutes} min"
                }
            } else {
                // If not numeric, show as-is
                recipe.cookTime
            }
        }

        holder.binding.imageViewThumbnail.load(recipe.imageUrl.ifBlank { null }) {
            placeholder(R.drawable.placeholder)
            error(R.drawable.placeholder)
            crossfade(true)
        }

        // Owner overflow menu (⋮)
        holder.binding.btnOverflow?.apply {
            visibility = if (showOwnerMenu) View.VISIBLE else View.GONE
            setOnClickListener { v ->
                PopupMenu(v.context, v).apply {
                    menuInflater.inflate(R.menu.menu_recipe_owner, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_edit -> { onEditClick?.invoke(recipe); true }
                            R.id.action_delete -> { onDeleteClick?.invoke(recipe); true }
                            else -> false
                        }
                    }
                }.show()
            }
        }


        // Bind main fields
        holder.binding.ratingBar.rating = recipe.averageRating()
        holder.binding.textViewRecipeName.text = recipe.name.ifEmpty { "Unnamed Recipe" }
        holder.binding.textViewDifficulty.text = recipe.difficulty
        holder.binding.textViewCookTime.text = cookTimeFormatted

        // Card click -> details
        holder.itemView.setOnClickListener {
            RecipeDetailActivity.start(holder.itemView.context, recipe)
        }
    }
    // edit / delete menu

    private var showOwnerMenu: Boolean = false
    fun setShowOwnerMenu(show: Boolean) { showOwnerMenu = show; notifyDataSetChanged() }

    var onEditClick: ((recipe: Recipe) -> Unit)? = null
    var onDeleteClick: ((recipe: Recipe) -> Unit)? = null

    override fun getItemCount(): Int = recipes.size

    fun updateList(newRecipes: List<Recipe>) {
        recipes.clear()
        recipes.addAll(newRecipes)
        notifyDataSetChanged()
    }
}

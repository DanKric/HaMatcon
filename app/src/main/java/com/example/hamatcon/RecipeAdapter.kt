package com.example.hamatcon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.hamatcon.databinding.ItemRecipeBinding

class RecipeAdapter(private val recipes: MutableList<Recipe>) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(val binding: ItemRecipeBinding) : RecyclerView.ViewHolder(binding.root)

    // ---- Favorites helpers ----
    private var favoriteIds: Set<String> = emptySet()
    fun setFavoriteIds(ids: Set<String>) { favoriteIds = ids; notifyDataSetChanged() }

    var onFavoriteClick: ((recipeId: String, isCurrentlyFav: Boolean) -> Unit)? = null
    // ---------------------------

    // show/hide favorites count on card
    private var showFavoritesCount: Boolean = false
    fun setShowFavoritesCount(show: Boolean) { showFavoritesCount = show; notifyDataSetChanged() }

    // show/hide "N ratings" label on card
    private var showRatingCount: Boolean = false
    fun setShowRatingCount(show: Boolean) { showRatingCount = show; notifyDataSetChanged() }

    // show/hide owner overflow menu (⋮)
    private var showOwnerMenu: Boolean = false
    fun setShowOwnerMenu(show: Boolean) { showOwnerMenu = show; notifyDataSetChanged() }

    var onEditClick: ((recipe: Recipe) -> Unit)? = null
    var onDeleteClick: ((recipe: Recipe) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        // --- Heart toggle ---
        holder.binding.btnFavorite?.let { btn ->
            val isFav = favoriteIds.contains(recipe.id)
            btn.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
            btn.setOnClickListener { onFavoriteClick?.invoke(recipe.id, isFav) }
        }

        // --- Favorites count (optional) ---
        holder.binding.textViewFavoritesCount?.apply {
            visibility = if (showFavoritesCount) View.VISIBLE else View.GONE
            text = recipe.favoritesCount.toString()
        }

        // --- "N ratings" label (optional) ---
        holder.binding.textViewRatingCount?.apply {
            visibility = if (showRatingCount) View.VISIBLE else View.GONE
            text = if (recipe.ratingCount == 1) "1 rating" else "${recipe.ratingCount} ratings"
        }

        // --- Pretty cook time ---
        val cookTimeFormatted = run {
            val numMatch = Regex("(\\d+)").find(recipe.cookTime)
            val minutes = numMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (minutes != null) {
                if (minutes >= 60) {
                    val h = minutes / 60
                    val m = minutes % 60
                    if (m == 0) "${h} hr" else "${h} hr ${m} min"
                } else {
                    "${minutes} min"
                }
            } else recipe.cookTime
        }

        // --- Image via Coil ---
        holder.binding.imageViewThumbnail.load(recipe.imageUrl.ifBlank { null }) {
            placeholder(R.drawable.placeholder)
            error(R.drawable.placeholder)
            crossfade(true)
        }

        // --- Owner overflow menu (⋮) ---
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

        // --- Bind main fields ---
        holder.binding.textViewRecipeName.text = recipe.name.ifEmpty { "Unnamed Recipe" }
        holder.binding.textViewDifficulty.text = recipe.difficulty
        holder.binding.textViewCookTime.text = cookTimeFormatted
        holder.binding.ratingBar.rating = recipe.averageRating() // stars under the heart per your layout

        // --- Card click -> details ---
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

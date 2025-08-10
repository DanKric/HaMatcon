package com.example.hamatcon

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hamatcon.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var recipesListener: ListenerRegistration? = null
    private var favListener: ListenerRegistration? = null
    private val favoriteIds = mutableSetOf<String>()

    private lateinit var recipeAdapter: RecipeAdapter
    private val allRecipes = mutableListOf<Recipe>()
    private var filteredRecipes = mutableListOf<Recipe>()
    private var selectedCuisine = "All"

    // Autocomplete + chips state
    private val selectedIngredients = linkedSetOf<String>()   // order, no dups
    private var ingredientIndex: List<String> = emptyList()   // autocomplete source

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Add Recipe
        binding.fabAddRecipe.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), AddRecipeActivity::class.java))
        }

        // Logout
        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Backfill (dev)
        binding.btnBackfill.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val updated = com.example.hamatcon.logic.RecipeAutofill.backfillSeededTimeAndDifficulty(
                        db = com.google.firebase.firestore.FirebaseFirestore.getInstance(),
                        ownerUidFilter = "seed"
                    )
                    android.widget.Toast.makeText(requireContext(), "Autofilled $updated recipes", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.util.Log.e("Autofill", "Failed", e)
                    android.widget.Toast.makeText(requireContext(), "Autofill failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }

        // Recycler
        recipeAdapter = RecipeAdapter(filteredRecipes).apply {
            setShowFavoritesCount(false) // hide count in Home
        }
        binding.recyclerViewRecipes.adapter = recipeAdapter
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(requireContext())

        // Favorite toggle from adapter
        recipeAdapter.onFavoriteClick = { recipeId, isCurrentlyFav ->
            toggleFavorite(recipeId, isCurrentlyFav)
        }

        // --- Autocomplete + chips wiring ---
        val selectedChipGroup: ChipGroup? = binding.root.findViewById(R.id.chipGroupSelected)
        val actv = binding.editTextSearch as MaterialAutoCompleteTextView

        // Pick from suggestions -> add chip
        actv.setOnItemClickListener { _, _, position, _ ->
            val value = actv.adapter.getItem(position) as String
            selectedChipGroup?.let { addIngredientChip(value, it) }
            actv.setText("")
        }

        // Enter/Done on keyboard -> turn typed text into a chip
        actv.setOnEditorActionListener { _, _, _ ->
            val raw = actv.text?.toString()?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                selectedChipGroup?.let { addIngredientChip(raw, it) }
                actv.setText("")
            }
            true
        }

        actv.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Hide hint if text is not empty
                actv.hint = if (s.isNullOrEmpty()) "Type an ingredient and select…" else ""
                filterRecipes()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }

    override fun onStart() {
        super.onStart()
        attachRecipesListener()
        attachFavoritesListener()
    }

    override fun onStop() {
        super.onStop()
        recipesListener?.remove(); recipesListener = null
        favListener?.remove(); favListener = null
    }

    private fun attachRecipesListener() {
        val db = Firebase.firestore
        recipesListener = db.collection("Recipes")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                allRecipes.clear()
                for (doc in snapshot) {
                    allRecipes.add(
                        Recipe(
                            name = doc.getString("name") ?: "",
                            matchPercent = doc.getLong("matchPercent")?.toInt() ?: 0,
                            difficulty = doc.getString("difficulty") ?: "",
                            cookTime = doc.getString("cookTime") ?: "",
                            thumbnailResId = R.drawable.placeholder,
                            cuisine = doc.getString("cuisine") ?: "",
                            ingredients = doc.get("ingredients") as? List<String> ?: emptyList(),
                            instructions = doc.getString("instructions") ?: "",
                            ratings = (doc.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            id = doc.id,
                            favoritesCount = doc.getLong("favoritesCount")?.toInt() ?: 0 // ✅ moved inside
                        )
                    )
                }

                // Build autocomplete index from normalized ingredients
                ingredientIndex = allRecipes
                    .flatMap { it.ingredients }
                    .map { normalizeIngredientName(it) }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                // Hook ACTV adapter
                val actv = binding.editTextSearch as MaterialAutoCompleteTextView
                actv.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        ingredientIndex
                    )
                )

                setupCuisineChips()
                filterRecipes()
            }
    }


    // Keep hearts in sync with user's favorites
    private fun attachFavoritesListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        favListener = Firebase.firestore
            .collection("users").document(uid)
            .collection("favorites")
            .addSnapshotListener { snap, _ ->
                favoriteIds.clear()
                snap?.forEach { favoriteIds.add(it.id) }
                recipeAdapter.setFavoriteIds(favoriteIds)
            }
    }

    // Toggle favorite + update aggregate count
    private fun toggleFavorite(recipeId: String, currentlyFav: Boolean) {
        val db = Firebase.firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val favRef = db.collection("users").document(uid)
            .collection("favorites").document(recipeId)
        val recipeRef = db.collection("Recipes").document(recipeId)

        db.runTransaction { tx ->
            if (currentlyFav) {
                tx.delete(favRef)
                tx.update(recipeRef, "favoritesCount", FieldValue.increment(-1))
            } else {
                tx.set(favRef, mapOf(
                    "saved" to true,
                    "ts" to FieldValue.serverTimestamp()
                ))
                tx.update(recipeRef, "favoritesCount", FieldValue.increment(1))
            }
            null
        }
    }

    private fun setupCuisineChips() {
        binding.chipGroupCuisine.removeAllViews()
        getAllCuisines().forEach { cuisine ->
            val chip = layoutInflater.inflate(R.layout.item_chip, binding.chipGroupCuisine, false) as com.google.android.material.chip.Chip
            chip.text = cuisine
            chip.isCheckable = true
            chip.isChecked = cuisine.equals(selectedCuisine, ignoreCase = true)
            chip.setOnClickListener {
                selectedCuisine = cuisine
                filterRecipes()
            }
            binding.chipGroupCuisine.addView(chip)
        }
    }

    private fun getAllCuisines(): List<String> {
        val cuisines = allRecipes.map { it.cuisine }.filter { it.isNotBlank() }.distinct().sorted()
        return listOf("All") + cuisines
    }

    private fun filterRecipes() {
        // Free‑text tokens are also normalized for consistency
        val tokens = parseQueryTokens((binding.editTextSearch as MaterialAutoCompleteTextView).text?.toString())
            .map { normalizeIngredientName(it) }
            .filter { it.isNotBlank() }

        filteredRecipes = allRecipes.filter { r ->
            val matchesCuisine = (selectedCuisine == "All" || r.cuisine.equals(selectedCuisine, true))

            // Normalize recipe ingredients for matching (strip numbers/units)
            val ingNorm = r.ingredients.map { normalizeIngredientName(it) }

            // AND across selected ingredient chips
            val matchesChips = if (selectedIngredients.isEmpty()) true
            else selectedIngredients.all { sel -> ingNorm.any { it.contains(sel) } }

            // Optional free‑text filter (also AND)
            val matchesFreeText = tokens.all { t -> ingNorm.any { it.contains(t) } }

            matchesCuisine && matchesChips && matchesFreeText
        }.toMutableList()

        recipeAdapter.updateList(filteredRecipes)
    }

    private fun parseQueryTokens(text: String?): List<String> =
        text?.lowercase()?.replace(",", " ")?.split(" ")
            ?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    // --- Helpers: normalize names & add chips ---

    // Remove numbers/units/punctuation -> keep just the ingredient words
    private fun normalizeIngredientName(s: String): String {
        val units = setOf(
            "lb","lbs","pound","pounds","kg","g","gram","grams","mg",
            "l","ml","liter","litre","cup","cups","tbsp","tsp","tablespoon","teaspoon",
            "oz","ounce","ounces","pinch","clove","cloves","slice","slices"
        )
        val lowered = s.lowercase()
            .replace(Regex("[0-9./-]+"), " ")      // remove numbers/fractions/ranges
            .replace(Regex("[()\\[\\],.:]"), " ")  // remove punctuation
        val cleaned = lowered.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in units }
            .joinToString(" ")
            .trim()
        return cleaned
    }

    private fun addIngredientChip(raw: String, chipGroup: ChipGroup) {
        val value = normalizeIngredientName(raw)
        if (value.isEmpty() || selectedIngredients.contains(value)) return

        selectedIngredients.add(value)

        val chip = Chip(requireContext()).apply {
            text = value
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedIngredients.remove(value)
                chipGroup.removeView(this)
                filterRecipes()
            }
        }
        chipGroup.addView(chip)
        filterRecipes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

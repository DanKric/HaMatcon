package com.example.hamatcon.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hamatcon.*
import com.example.hamatcon.databinding.FragmentHomeBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var recipesListener: ListenerRegistration? = null
    private lateinit var recipeAdapter: RecipeAdapter
    private val allRecipes = mutableListOf<Recipe>()
    private var filteredRecipes = mutableListOf<Recipe>()
    private var selectedCuisine = "All"

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
                        db = FirebaseFirestore.getInstance(),
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
        recipeAdapter = RecipeAdapter(filteredRecipes)
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewRecipes.adapter = recipeAdapter

        // Search
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = filterRecipes()
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        attachRecipesListener()
    }

    override fun onStop() {
        super.onStop()
        recipesListener?.remove()
        recipesListener = null
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
                            ratings = (doc.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                        )
                    )
                }
                setupCuisineChips()
                filterRecipes()
            }
    }

    private fun setupCuisineChips() {
        binding.chipGroupCuisine.removeAllViews()
        getAllCuisines().forEach { cuisine ->
            val chip = layoutInflater.inflate(R.layout.item_chip, binding.chipGroupCuisine, false) as Chip
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
        val tokens = parseQueryTokens(binding.editTextSearch.text?.toString())
        filteredRecipes = allRecipes.filter { r ->
            val matchesCuisine = (selectedCuisine == "All" || r.cuisine.equals(selectedCuisine, true))
            val ingredientsLower = r.ingredients.map { it.lowercase() }
            val matchesIngredients = tokens.all { t -> ingredientsLower.any { ing -> ing.contains(t) } }
            matchesCuisine && matchesIngredients
        }.toMutableList()
        recipeAdapter.updateList(filteredRecipes)
    }

    private fun parseQueryTokens(text: String?): List<String> =
        text?.lowercase()?.replace(",", " ")?.split(" ")
            ?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

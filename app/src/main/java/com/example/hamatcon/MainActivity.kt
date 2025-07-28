package com.example.hamatcon

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hamatcon.databinding.ActivityMainBinding
import com.example.hamatcon.databinding.ItemRecipeBinding
import com.example.hamatcon.Recipe
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

import android.util.Log


// Data class for Recipe

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private val allRecipes = listOf(
        Recipe(
            name = "Spaghetti Carbonara",
            matchPercent = 80,
            difficulty = "Easy",
            cookTime = "20 min",
            thumbnailResId = R.drawable.placeholder,
            cuisine = "Italian",
            ingredients = listOf("spaghetti", "egg", "cheese", "bacon"),
            instructions = "1. Cook spaghetti.\n2. Mix eggs and cheese.\n3. Fry bacon.\n4. Combine all and serve.",
            ratings = listOf(5, 4, 5, 5)
        ),
        Recipe(
            name = "Sushi Rolls",
            matchPercent = 60,
            difficulty = "Medium",
            cookTime = "45 min",
            thumbnailResId = R.drawable.placeholder,
            cuisine = "Asian",
            ingredients = listOf("rice", "nori", "fish", "cucumber"),
            instructions = "1. Cook sushi rice.\n2. Lay out nori.\n3. Add rice and fillings.\n4. Roll tightly and slice.",
            ratings = listOf(4, 4, 5, 3)
        ),
        Recipe(
            name = "Shakshuka",
            matchPercent = 90,
            difficulty = "Easy",
            cookTime = "30 min",
            thumbnailResId = R.drawable.placeholder,
            cuisine = "Israeli",
            ingredients = listOf("egg", "tomato", "pepper", "onion"),
            instructions = "1. Sauté onions and peppers.\n2. Add tomatoes and spices.\n3. Crack eggs on top.\n4. Cook until eggs are set.",
            ratings = listOf(5, 5, 4, 5)
        ),
        Recipe(
            name = "Greek Salad",
            matchPercent = 70,
            difficulty = "Easy",
            cookTime = "15 min",
            thumbnailResId = R.drawable.placeholder,
            cuisine = "Mediterranean",
            ingredients = listOf("tomato", "cucumber", "feta", "olive"),
            instructions = "1. Chop veggies.\n2. Add feta and olives.\n3. Drizzle olive oil and vinegar.\n4. Toss and serve.",
            ratings = listOf(3, 4, 4, 3)
        ),
        Recipe(
            name = "Pad Thai",
            matchPercent = 50,
            difficulty = "Hard",
            cookTime = "40 min",
            thumbnailResId = R.drawable.placeholder,
            cuisine = "Asian",
            ingredients = listOf("noodle", "egg", "peanut", "shrimp"),
            instructions = "1. Soak rice noodles.\n2. Stir-fry garlic and proteins.\n3. Add sauce and noodles.\n4. Toss with peanuts.",
            ratings = listOf(5, 3, 4, 4)
        )
    )

    private var filteredRecipes = allRecipes.toMutableList()
    private val cuisineTypes = listOf("All", "Asian", "Italian", "Israeli", "Mediterranean")
    private var selectedCuisine = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val db = Firebase.firestore
        db.collection("recipes")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    Log.d("FirestoreTest", "${doc.id} => ${doc.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.w("FirestoreTest", "Error getting documents", e)
            }


        // Set up RecyclerView
        recipeAdapter = RecipeAdapter(filteredRecipes)
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = recipeAdapter

        // Set up cuisine filter chips
        setupCuisineChips()

        // Set up search bar
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCuisineChips() {
        binding.chipGroupCuisine.removeAllViews()
        for (cuisine in cuisineTypes) {
            val chip = LayoutInflater.from(this).inflate(R.layout.item_chip, binding.chipGroupCuisine, false) as com.google.android.material.chip.Chip
            chip.text = cuisine
            chip.isChecked = cuisine == selectedCuisine
            chip.setOnClickListener {
                selectedCuisine = cuisine
                filterRecipes()
            }
            binding.chipGroupCuisine.addView(chip)
        }
    }

    private fun filterRecipes() {
        val query = binding.editTextSearch.text.toString().trim().lowercase()
        filteredRecipes = allRecipes.filter { recipe ->
            val matchesCuisine = (selectedCuisine == "All" || recipe.cuisine == selectedCuisine)
            val matchesQuery = query.isEmpty() || recipe.ingredients.any { it.contains(query) }
            matchesCuisine && matchesQuery
        }.toMutableList()
        recipeAdapter.updateList(filteredRecipes)
    }
}
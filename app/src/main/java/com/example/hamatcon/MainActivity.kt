package com.example.hamatcon

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hamatcon.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.Menu
import android.view.MenuItem
import com.example.hamatcon.ui.HomeFragment
import com.example.hamatcon.ui.MyRecipesFragment
import com.example.hamatcon.ui.ProfileFragment

import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.example.hamatcon.logic.RecipeAutofill




// Data class for Recipe

class MainActivity : AppCompatActivity() {
    private var recipesListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private val allRecipes = mutableListOf<Recipe>()

    private lateinit var homeFragment: HomeFragment
    private lateinit var myRecipesFragment: MyRecipesFragment
    private lateinit var profileFragment: ProfileFragment

    private var filteredRecipes = allRecipes.toMutableList()
    private var selectedCuisine = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup view binding first
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.btnBackfill.visibility = View.VISIBLE

        binding.btnBackfill.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val updated = RecipeAutofill.backfillSeededTimeAndDifficulty(
                        db = FirebaseFirestore.getInstance(),
                        ownerUidFilter = "seed"
                    )
                    Toast.makeText(this@MainActivity, "Autofilled $updated recipes", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("Autofill", "Failed", e)
                    Toast.makeText(this@MainActivity, "Autofill failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }


        // ✅ Hook up logout using binding!
        binding.logoutButton.setOnClickListener {
            Log.d("AuthDebug", "Logout button clicked")
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val fab = findViewById<FloatingActionButton>(R.id.fabAddRecipe)
        fab.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
        }

        // Set up RecyclerView
        recipeAdapter = RecipeAdapter(filteredRecipes)
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = recipeAdapter


        // Set up search bar
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        initBottomNav()

    }

    private fun attachRecipesListener() {
        val db = Firebase.firestore
        recipesListener = db.collection("Recipes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirestoreError", "Listen failed", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                allRecipes.clear()
                for (document in snapshot) {
                    val recipe = Recipe(
                        name = document.getString("name") ?: "",
                        matchPercent = document.getLong("matchPercent")?.toInt() ?: 0,
                        difficulty = document.getString("difficulty") ?: "",
                        cookTime = document.getString("cookTime") ?: "",
                        thumbnailResId = R.drawable.placeholder,
                        cuisine = document.getString("cuisine") ?: "",
                        ingredients = document.get("ingredients") as? List<String> ?: emptyList(),
                        instructions = document.getString("instructions") ?: "",
                        ratings = (document.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                    )
                    allRecipes.add(recipe)
                }

                // Refresh UI
                filterRecipes()
                setupCuisineChips()
                Log.d("RecipeDebug", "Realtime list size: ${allRecipes.size}")

            }
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



    private fun setupCuisineChips() {
        binding.chipGroupCuisine.removeAllViews()

        val cuisines = getAllCuisines()
        for (cuisine in cuisines) {
            val chip = LayoutInflater.from(this)
                .inflate(R.layout.item_chip, binding.chipGroupCuisine, false) as com.google.android.material.chip.Chip
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
        val cuisines = allRecipes
            .map { it.cuisine }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return listOf("All") + cuisines
    }


    private fun filterRecipes() {
        val tokens = parseQueryTokens(binding.editTextSearch.text?.toString())
        filteredRecipes = allRecipes.filter { recipe ->
            // Cuisine match (case-insensitive)
            val matchesCuisine = (selectedCuisine == "All" ||
                    recipe.cuisine.equals(selectedCuisine, ignoreCase = true))

            // Ingredient AND search (case-insensitive, substring match)
            val ingredientsLower = recipe.ingredients.map { it.lowercase() }
            val matchesIngredients = tokens.all { token ->
                ingredientsLower.any { ing -> ing.contains(token) }
            }

            matchesCuisine && matchesIngredients
        }.toMutableList()
        recipeAdapter.updateList(filteredRecipes)
    }

    private fun parseQueryTokens(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        return text.lowercase()
            .replace(",", " ")        // allow comma or space separation
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_seed -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initBottomNav() {
        // Default = Home visible, fragments hidden
        setHomeMode(true)

        // If you want to attach a fragment for Home too, you can,
        // but we’ll keep the Activity UI as Home for now.

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    setHomeMode(true)
                    true
                }
                R.id.nav_my_recipes -> {
                    setHomeMode(false)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, MyRecipesFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    setHomeMode(false)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Make sure Home is selected initially
        binding.bottomNav.selectedItemId = R.id.nav_home
    }


    private fun setHomeMode(isHome: Boolean) {
        // Home UI (Activity-owned views)
        binding.searchInputLayout.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.chipScroll.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.recyclerViewRecipes.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.fabAddRecipe.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.btnBackfill.visibility = if (isHome) View.VISIBLE else View.GONE
        binding.logoutButton.visibility = if (isHome) View.VISIBLE else View.GONE

        // Fragment container
        binding.fragmentContainer.visibility = if (isHome) View.GONE else View.VISIBLE
    }


}
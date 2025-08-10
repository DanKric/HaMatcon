package com.example.hamatcon

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hamatcon.databinding.ActivityAddRecipeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecipeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSubmit.setOnClickListener {
            val name = binding.editName.text.toString().trim()
            val cuisine = binding.editCuisine.text.toString().trim()
            val cookTime = binding.editCookTime.text.toString().trim()
            val difficulty = binding.editDifficulty.text.toString().trim()

            // Normalize ingredients: split, trim, lowercase, dedupe, drop empties
            val ingredients = binding.editIngredients.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.lowercase() }
                .distinct()

            val instructions = binding.editInstructions.text.toString().trim()

            // Require a logged-in user for creation
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() || cuisine.isEmpty() || cookTime.isEmpty() || ingredients.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val recipe = hashMapOf(
                "name" to name,
                "cuisine" to cuisine,
                "cookTime" to cookTime,
                "difficulty" to difficulty,
                "ingredients" to ingredients,
                "instructions" to instructions,
                "ownerUid" to uid,

                // helpful defaults
                "imageUrl" to "",                 // to be set when we add uploads
                "favoritesCount" to 0,            // used by Favorites logic
                "ratings" to listOf<Int>(),       // keep existing shape
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            Firebase.firestore.collection("Recipes")
                .add(recipe)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }
}
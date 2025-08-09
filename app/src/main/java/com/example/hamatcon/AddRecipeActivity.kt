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
            val ingredients = binding.editIngredients.text.toString()
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val instructions = binding.editInstructions.text.toString().trim()
            val uid = FirebaseAuth.getInstance().currentUser?.uid


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
                "matchPercent" to 100,
                "ratings" to listOf<Int>(),
                "ownerUid" to (uid ?: "seed")
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

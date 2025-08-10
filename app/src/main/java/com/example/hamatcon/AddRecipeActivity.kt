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

    private val db by lazy { Firebase.firestore }
    private val uid by lazy { FirebaseAuth.getInstance().currentUser?.uid }

    private var isEditMode = false
    private var recipeId: String? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detect edit mode
        isEditMode = intent.getStringExtra("mode") == "edit"
        recipeId = intent.getStringExtra("recipeId")

        if (isEditMode && !recipeId.isNullOrBlank()) {
            title = "Edit Recipe"
            binding.buttonSubmit.text = "Save"

            // Prefill from Intent extras (fast path)
            binding.editName.setText(intent.getStringExtra("name") ?: "")
            binding.editCuisine.setText(intent.getStringExtra("cuisine") ?: "")
            binding.editCookTime.setText(intent.getStringExtra("cookTime") ?: "")
            binding.editDifficulty.setText(intent.getStringExtra("difficulty") ?: "")
            val ingFromExtras = intent.getStringArrayListExtra("ingredients") ?: arrayListOf()
            if (ingFromExtras.isNotEmpty()) {
                binding.editIngredients.setText(ingFromExtras.joinToString(", "))
            }
            binding.editInstructions.setText(intent.getStringExtra("instructions") ?: "")

            // Optional: if extras were partial or stale, load latest from Firestore
            if (binding.editName.text.isNullOrBlank()) {
                loadRecipeFromFirestore(recipeId!!)
            }

            // Lock fields you don't want to change in edit mode (optional):
            // binding.editCuisine.isEnabled = false
            // binding.editDifficulty.isEnabled = false
        } else {
            title = "Add Recipe"
            binding.buttonSubmit.text = "Add"
        }

        binding.buttonSubmit.setOnClickListener { onSubmit() }
    }

    private fun loadRecipeFromFirestore(id: String) {
        db.collection("Recipes").document(id).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener
                binding.editName.setText(doc.getString("name") ?: "")
                binding.editCuisine.setText(doc.getString("cuisine") ?: "")
                binding.editCookTime.setText(doc.getString("cookTime") ?: "")
                binding.editDifficulty.setText(doc.getString("difficulty") ?: "")
                val ings = (doc.get("ingredients") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                if (ings.isNotEmpty()) binding.editIngredients.setText(ings.joinToString(", "))
                binding.editInstructions.setText(doc.getString("instructions") ?: "")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun onSubmit() {
        // Basic auth + validation
        if (uid == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.editName.text.toString().trim()
        val cuisine = binding.editCuisine.text.toString().trim()
        val cookTime = binding.editCookTime.text.toString().trim()
        val difficulty = binding.editDifficulty.text.toString().trim()
        val instructions = binding.editInstructions.text.toString().trim()

        // Normalize ingredients: split on comma/newline/semicolon
        val ingredients = binding.editIngredients.text.toString()
            .split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }
            .distinct()

        if (name.isEmpty() || cuisine.isEmpty() || cookTime.isEmpty() || ingredients.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val data = mapOf(
            "name" to name,
            "cuisine" to cuisine,
            "cookTime" to cookTime,
            "difficulty" to difficulty,
            "ingredients" to ingredients,
            "instructions" to instructions
        )

        if (isEditMode && !recipeId.isNullOrBlank()) {
            // UPDATE existing doc (do not touch ownerUid / favoritesCount / ratings)
            db.collection("Recipes").document(recipeId!!)
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // CREATE new doc
            val newData = data + mapOf(
                "ownerUid" to uid,
                "favoritesCount" to 0,
                "ratings" to emptyList<Int>(),
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "imageUrl" to "" // to be filled when image upload is added
            )
            db.collection("Recipes")
                .add(newData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}

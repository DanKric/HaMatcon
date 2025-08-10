package com.example.hamatcon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.hamatcon.databinding.ActivityAddRecipeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddRecipeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddRecipeBinding

    // Firebase
    private val db by lazy { Firebase.firestore }
    private val storage by lazy { Firebase.storage }
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // Mode
    private var isEditMode = false
    private var recipeId: String? = null

    // Image state
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    // System picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* ignore */ }

            selectedImageUri = uri
            binding.imagePreview.load(uri) {
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
                crossfade(true)
            }
            binding.imageHint.alpha = 0f
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecipeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detect mode + params
        isEditMode = intent.getStringExtra("mode") == "edit"
        recipeId = intent.getStringExtra("recipeId")

        if (isEditMode && !recipeId.isNullOrBlank()) {
            title = "Edit Recipe"
            binding.buttonSubmit.text = "Save"

            // Prefill from Intent extras (if provided)
            binding.editName.setText(intent.getStringExtra("name") ?: "")
            binding.editCuisine.setText(intent.getStringExtra("cuisine") ?: "")
            binding.editCookTime.setText(intent.getStringExtra("cookTime") ?: "")
            binding.editDifficulty.setText(intent.getStringExtra("difficulty") ?: "")
            intent.getStringArrayListExtra("ingredients")
                ?.takeIf { it.isNotEmpty() }
                ?.let { binding.editIngredients.setText(it.joinToString(", ")) }
            binding.editInstructions.setText(intent.getStringExtra("instructions") ?: "")
            existingImageUrl = intent.getStringExtra("imageUrl") ?: ""

            if (existingImageUrl.isNotBlank()) {
                binding.imagePreview.load(existingImageUrl) {
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                    crossfade(true)
                }
                binding.imageHint.alpha = 0f
            }
        } else {
            title = "Add Recipe"
            binding.buttonSubmit.text = "Add"
        }

        // Add image button
        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        // Save
        binding.buttonSubmit.setOnClickListener { onSubmit() }
    }

    private fun onSubmit() {
        val userId = uid ?: run {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.editName.text?.toString()?.trim().orEmpty()
        val cuisine = binding.editCuisine.text?.toString()?.trim().orEmpty()
        val cookTime = binding.editCookTime.text?.toString()?.trim().orEmpty()
        val difficulty = binding.editDifficulty.text?.toString()?.trim().orEmpty()
        val instructions = binding.editInstructions.text?.toString()?.trim().orEmpty()
        val ingredients = binding.editIngredients.text?.toString()
            ?.split(",", "\n", ";")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { it.lowercase() }
            ?.distinct()
            ?: emptyList()

        if (name.isEmpty() || cuisine.isEmpty() || cookTime.isEmpty() || ingredients.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode && !recipeId.isNullOrBlank()) {
            saveEdit(
                userId, recipeId!!,
                name, cuisine, cookTime, difficulty, ingredients, instructions
            )
        } else {
            saveNew(
                userId,
                name, cuisine, cookTime, difficulty, ingredients, instructions
            )
        }
    }

    // ------------------- EDIT -------------------
    private fun saveEdit(
        userId: String,
        rid: String,
        name: String,
        cuisine: String,
        cookTime: String,
        difficulty: String,
        ingredients: List<String>,
        instructions: String
    ) {
        val update = hashMapOf(
            "name" to name,
            "cuisine" to cuisine,
            "cookTime" to cookTime,
            "difficulty" to difficulty,
            "ingredients" to ingredients,
            "instructions" to instructions
        ) as MutableMap<String, Any>

        val newUri = selectedImageUri
        if (newUri != null) {
            val ref = storage.reference.child("recipes/$userId/$rid.jpg")
            android.util.Log.d("UPLOAD", "Editing: PUT -> ${ref.path}  uri=$newUri")

            ref.putFile(newUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                    ref.downloadUrl
                }
                .addOnSuccessListener { url ->
                    update["imageUrl"] = url.toString()
                    pushUpdate(rid, update)
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("UPLOAD", "Edit upload failed", e)
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            pushUpdate(rid, update) // keep existing image
        }
    }

    private fun pushUpdate(rid: String, data: Map<String, Any>) {
        db.collection("Recipes").document(rid)
            .update(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Recipe updated", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ------------------- CREATE -------------------
    private fun saveNew(
        userId: String,
        name: String,
        cuisine: String,
        cookTime: String,
        difficulty: String,
        ingredients: List<String>,
        instructions: String
    ) {
        val base = hashMapOf(
            "name" to name,
            "cuisine" to cuisine,
            "cookTime" to cookTime,
            "difficulty" to difficulty,
            "ingredients" to ingredients,
            "instructions" to instructions,
            "ownerUid" to userId,
            "favoritesCount" to 0,
            "ratings" to emptyList<Int>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "imageUrl" to ""
        )

        db.collection("Recipes").add(base)
            .addOnSuccessListener { docRef ->
                val newId = docRef.id
                val uri = selectedImageUri
                if (uri != null) {
                    val ref = storage.reference.child("recipes/$userId/$newId.jpg")
                    android.util.Log.d("UPLOAD", "Create: PUT -> ${ref.path}  uri=$uri")

                    ref.putFile(uri)
                        .continueWithTask { task ->
                            if (!task.isSuccessful) throw task.exception ?: Exception("Upload failed")
                            ref.downloadUrl
                        }
                        .addOnSuccessListener { url ->
                            docRef.update("imageUrl", url.toString())
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("UPLOAD", "URL write failed", e)
                                    Toast.makeText(this, "Saved but image URL write failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    finish()
                                }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("UPLOAD", "Create upload failed", e)
                            Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                            // Recipe doc exists without image; user can edit later to add one
                            finish()
                        }
                } else {
                    Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

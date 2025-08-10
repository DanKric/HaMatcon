package com.example.hamatcon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.widget.TextView

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {

    private lateinit var adapter: RecipeAdapter
    private val myRecipes = mutableListOf<Recipe>()

    private var recipesListener: ListenerRegistration? = null
    private var favListener: ListenerRegistration? = null
    private val favoriteIds = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMy)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipeAdapter(mutableListOf())
        rv.adapter = adapter

        // Allow toggling favorites here too (remove if you want read‑only)
        adapter.onFavoriteClick = { recipeId, isCurrentlyFav ->
            toggleFavorite(recipeId, isCurrentlyFav)
        }
    }

    override fun onStart() {
        super.onStart()
        attachFavoritesListener()
        attachMyRecipesListener()
    }

    override fun onStop() {
        super.onStop()
        recipesListener?.remove(); recipesListener = null
        favListener?.remove(); favListener = null
    }

    private fun attachFavoritesListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        favListener = Firebase.firestore.collection("users").document(uid)
            .collection("favorites")
            .addSnapshotListener { snap, _ ->
                favoriteIds.clear()
                snap?.forEach { favoriteIds.add(it.id) }
                adapter.setFavoriteIds(favoriteIds)
            }
    }

    private fun attachMyRecipesListener() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            android.widget.Toast.makeText(requireContext(), "Not logged in", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val db = Firebase.firestore
        recipesListener?.remove()

        // Primary query: ownerUid == uid ORDER BY name (uses composite index)
        recipesListener = db.collection("Recipes")
            .whereEqualTo("ownerUid", uid)
            .orderBy("name", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.w("MyRecipes", "Listen failed (with orderBy): ${e.message}", e)

                    // Fallback: try again WITHOUT orderBy so you can see results while we diagnose
                    db.collection("Recipes")
                        .whereEqualTo("ownerUid", uid)
                        .addSnapshotListener { snap2, e2 ->
                            if (e2 != null) {
                                android.util.Log.e("MyRecipes", "Fallback also failed: ${e2.message}", e2)
                                return@addSnapshotListener
                            }
                            android.util.Log.d("MyRecipes", "Fallback snapshot size=${snap2?.size() ?: 0}, uid=$uid")
                            updateMyRecipesFromSnapshot(snap2)
                        }
                    return@addSnapshotListener
                }

                android.util.Log.d("MyRecipes", "Primary snapshot size=${snapshot?.size() ?: 0}, uid=$uid")
                updateMyRecipesFromSnapshot(snapshot)
            }
    }

    private fun updateMyRecipesFromSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot?) {
        myRecipes.clear()
        snapshot?.forEach { doc ->
            val r = Recipe(
                name = doc.getString("name") ?: "",
                matchPercent = doc.getLong("matchPercent")?.toInt() ?: 0,
                difficulty = doc.getString("difficulty") ?: "",
                cookTime = doc.getString("cookTime") ?: "",
                thumbnailResId = R.drawable.placeholder,
                cuisine = doc.getString("cuisine") ?: "",
                ingredients = doc.get("ingredients") as? List<String> ?: emptyList(),
                instructions = doc.getString("instructions") ?: "",
                ratings = (doc.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                id = doc.id
            )
            android.util.Log.d("MyRecipes", "Bind: ${r.name} (${r.id})")
            myRecipes.add(r)
        }
        adapter.updateList(myRecipes)
        android.util.Log.d("MyRecipes", "Adapter count=${adapter.itemCount}")


        val empty = view?.findViewById<TextView>(R.id.emptyText)
        empty?.visibility = if (myRecipes.isEmpty()) View.VISIBLE else View.GONE

    }


    private fun toggleFavorite(recipeId: String, currentlyFav: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore
        val favRef = db.collection("users").document(uid)
            .collection("favorites").document(recipeId)
        val recipeRef = db.collection("Recipes").document(recipeId)

        db.runTransaction { tx ->
            if (currentlyFav) {
                tx.delete(favRef)
                tx.update(recipeRef, "favoritesCount", com.google.firebase.firestore.FieldValue.increment(-1))
            } else {
                tx.set(favRef, mapOf(
                    "saved" to true,
                    "ts" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
                tx.update(recipeRef, "favoritesCount", com.google.firebase.firestore.FieldValue.increment(1))
            }
            null
        }
    }
}

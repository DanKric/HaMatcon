package com.example.hamatcon

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FavoritesFragment : Fragment(R.layout.fragment_my_recipes) {

    private lateinit var adapter: RecipeAdapter

    // Live state
    private val recipeMap = linkedMapOf<String, Recipe>()           // id -> Recipe (preserves insertion order)
    private var favIdsOrdered: List<String> = emptyList()           // ordered by ts desc
    private val chunkListeners = mutableListOf<ListenerRegistration>()
    private var favListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMy)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipeAdapter(mutableListOf())
        rv.adapter = adapter

        // Hearts should work here too
        adapter.onFavoriteClick = { recipeId, isCurrentlyFav ->
            toggleFavorite(recipeId, isCurrentlyFav)
        }
    }

    override fun onStart() {
        super.onStart()
        attachFavorites()
    }

    override fun onStop() {
        super.onStop()
        favListener?.remove(); favListener = null
        chunkListeners.forEach { it.remove() }
        chunkListeners.clear()
    }

    private fun attachFavorites() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = Firebase.firestore

        favListener?.remove()
        favListener = db.collection("users").document(uid)
            .collection("favorites")
            .orderBy("ts", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                // 1) Update adapter’s heart state
                val ids = snap?.documents?.map { it.id } ?: emptyList()
                adapter.setFavoriteIds(ids.toSet())

                // 2) Keep display order by ts
                favIdsOrdered = ids

                // 3) Load recipes in chunks
                loadFavoriteRecipes(ids)
            }
    }

    private fun loadFavoriteRecipes(ids: List<String>) {
        // Clear old recipe listeners
        chunkListeners.forEach { it.remove() }
        chunkListeners.clear()

        recipeMap.clear()
        adapter.updateList(emptyList())

        if (ids.isEmpty()) return

        val db = Firebase.firestore
        ids.chunked(10).forEach { chunk ->
            val reg = db.collection("Recipes")
                .whereIn(FieldPath.documentId(), chunk)
                .addSnapshotListener { snap, _ ->
                    // Merge into map
                    snap?.documents?.forEach { d ->
                        val r = Recipe(
                            name = d.getString("name") ?: "",
                            matchPercent = d.getLong("matchPercent")?.toInt() ?: 0,
                            difficulty = d.getString("difficulty") ?: "",
                            cookTime = d.getString("cookTime") ?: "",
                            thumbnailResId = R.drawable.placeholder,
                            cuisine = d.getString("cuisine") ?: "",
                            ingredients = d.get("ingredients") as? List<String> ?: emptyList(),
                            instructions = d.getString("instructions") ?: "",
                            ratings = (d.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList(),
                            id = d.id
                        )
                        recipeMap[d.id] = r
                    }

                    // Rebuild list in favored order (ts desc)
                    val ordered = favIdsOrdered.mapNotNull { recipeMap[it] }
                    adapter.updateList(ordered)
                }
            chunkListeners.add(reg)
        }
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
                tx.update(recipeRef, "favoritesCount", FieldValue.increment(-1))
            } else {
                tx.set(favRef, mapOf("saved" to true, "ts" to FieldValue.serverTimestamp()))
                tx.update(recipeRef, "favoritesCount", FieldValue.increment(1))
            }
            null
        }
    }
}

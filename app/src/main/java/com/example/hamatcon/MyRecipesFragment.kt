package com.example.hamatcon.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hamatcon.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyRecipesFragment : Fragment(R.layout.fragment_my_recipes) {
    private val myList = mutableListOf<Recipe>()
    private lateinit var adapter: RecipeAdapter
    private var listener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerMy)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipeAdapter(myList)
        rv.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        listener = Firebase.firestore.collection("Recipes")
            .whereEqualTo("ownerUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                myList.clear()
                for (d in snap) {
                    myList.add(
                        Recipe(
                            name = d.getString("name") ?: "",
                            matchPercent = d.getLong("matchPercent")?.toInt() ?: 0,
                            difficulty = d.getString("difficulty") ?: "",
                            cookTime = d.getString("cookTime") ?: "",
                            thumbnailResId = R.drawable.placeholder,
                            cuisine = d.getString("cuisine") ?: "",
                            ingredients = d.get("ingredients") as? List<String> ?: emptyList(),
                            instructions = d.getString("instructions") ?: "",
                            ratings = (d.get("ratings") as? List<Long>)?.map { it.toInt() } ?: emptyList()
                        )
                    )
                }
                adapter.updateList(myList.toMutableList())
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }
}

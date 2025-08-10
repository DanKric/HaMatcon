package com.example.hamatcon

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.example.hamatcon.LoginActivity
import com.example.hamatcon.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Find views
        val emailTv = view.findViewById<TextView>(R.id.tvEmail)
        val nameTv = view.findViewById<TextView>(R.id.tvName)
        val recipeCountTv = view.findViewById<TextView>(R.id.tvRecipeCount)
        val favoriteCountTv = view.findViewById<TextView>(R.id.tvFavoriteCount)
        val lastRecipeDateTv = view.findViewById<TextView>(R.id.tvLastRecipeDate)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)
        val editProfileBtn = view.findViewById<MaterialButton>(R.id.btnEditProfile)
        val settingsBtn = view.findViewById<MaterialButton>(R.id.btnSettings)

        // Set user info
        val user = auth.currentUser
        emailTv.text = user?.email ?: "No email"
        nameTv.text = user?.displayName ?: "No name set"

        // Load user statistics
        loadUserStatistics(recipeCountTv, favoriteCountTv, lastRecipeDateTv)

        // Set button listeners
        logoutBtn.setOnClickListener {
            performLogout()
        }

        editProfileBtn.setOnClickListener {
            // TODO: Implement edit profile functionality
            Toast.makeText(requireContext(), "Edit Profile - Coming soon!", Toast.LENGTH_SHORT).show()
        }

        settingsBtn.setOnClickListener {
            // TODO: Implement settings functionality
            Toast.makeText(requireContext(), "Settings - Coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserStatistics(
        recipeCountTv: TextView,
        favoriteCountTv: TextView,
        lastRecipeDateTv: TextView
    ) {
        val user = auth.currentUser
        if (user == null) {
            recipeCountTv.text = "0"
            favoriteCountTv.text = "0"
            lastRecipeDateTv.text = "Never"
            return
        }

        val userId = user.uid

        // Load total recipe count
        firestore.collection("recipes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                recipeCountTv.text = documents.size().toString()
            }
            .addOnFailureListener {
                recipeCountTv.text = "0"
            }

        // Load favorite recipe count
        firestore.collection("recipes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isFavorite", true)
            .get()
            .addOnSuccessListener { documents ->
                favoriteCountTv.text = documents.size().toString()
            }
            .addOnFailureListener {
                favoriteCountTv.text = "0"
            }

        // Load last recipe date
        firestore.collection("recipes")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    lastRecipeDateTv.text = "Never"
                } else {
                    val lastRecipe = documents.first()
                    val createdAt = lastRecipe.getTimestamp("createdAt")
                    if (createdAt != null) {
                        val date = createdAt.toDate()
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val today = Calendar.getInstance()
                        val recipeDate = Calendar.getInstance().apply { time = date }

                        when {
                            isSameDay(today, recipeDate) -> {
                                lastRecipeDateTv.text = "Today"
                            }
                            isYesterday(today, recipeDate) -> {
                                lastRecipeDateTv.text = "Yesterday"
                            }
                            else -> {
                                lastRecipeDateTv.text = dateFormat.format(date)
                            }
                        }
                    } else {
                        lastRecipeDateTv.text = "Unknown"
                    }
                }
            }
            .addOnFailureListener {
                lastRecipeDateTv.text = "Unknown"
            }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun isYesterday(today: Calendar, date: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, date)
    }

    private fun performLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh statistics when fragment becomes visible
        val recipeCountTv = view?.findViewById<TextView>(R.id.tvRecipeCount)
        val favoriteCountTv = view?.findViewById<TextView>(R.id.tvFavoriteCount)
        val lastRecipeDateTv = view?.findViewById<TextView>(R.id.tvLastRecipeDate)

        if (recipeCountTv != null && favoriteCountTv != null && lastRecipeDateTv != null) {
            loadUserStatistics(recipeCountTv, favoriteCountTv, lastRecipeDateTv)
        }
    }
}
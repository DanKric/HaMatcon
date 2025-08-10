package com.example.hamatcon

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.EmailAuthProvider
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

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Views
        val emailTv = view.findViewById<TextView>(R.id.tvEmail)
        val recipeCountTv = view.findViewById<TextView>(R.id.tvRecipeCount)
        val favoriteCountTv = view.findViewById<TextView>(R.id.tvFavoriteCount)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)

        // User info
        val user = auth.currentUser
        emailTv.text = user?.email ?: "No email"

        // Logout
        logoutBtn.setOnClickListener { performLogout() }

        // Load stats
        loadUserStatistics(recipeCountTv, favoriteCountTv)
    }

    /**
     * Loads:
     * 1) Total recipes owned by the current user.
     * 2) Favorites count that matches Favorites tab by ignoring stale favorites.
     */
    private fun loadUserStatistics(
        recipeCountTv: TextView,
        favoriteCountTv: TextView
    ) {
        val user = auth.currentUser ?: run {
            recipeCountTv.text = "0"
            favoriteCountTv.text = "0"
            return
        }
        val uid = user.uid

        // Total recipes owned
        firestore.collection("Recipes")
            .whereEqualTo("ownerUid", uid)
            .get()
            .addOnSuccessListener { snap -> recipeCountTv.text = snap.size().toString() }
            .addOnFailureListener { recipeCountTv.text = "0" }

        // Favorites count matching Favorites tab
        firestore.collection("users").document(uid).collection("favorites")
            .get()
            .addOnSuccessListener { favSnap ->
                if (favSnap.isEmpty) {
                    favoriteCountTv.text = "0"
                    return@addOnSuccessListener
                }

                var existing = 0
                var processed = 0
                val total = favSnap.size()

                favSnap.documents.forEach { fdoc ->
                    val recipeId = fdoc.id
                    firestore.collection("Recipes").document(recipeId).get()
                        .addOnSuccessListener { r ->
                            if (r.exists()) existing++
                        }
                        .addOnCompleteListener {
                            processed++
                            if (processed == total) {
                                favoriteCountTv.text = existing.toString()
                            }
                        }
                }
            }
            .addOnFailureListener { favoriteCountTv.text = "0" }
    }

    private fun performLogout() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showEditEmailDialog(emailTv: TextView) {
        val ctx = requireContext()
        val container = View.inflate(ctx, R.layout.view_edit_email, null)

        val emailLayout = container.findViewById<TextInputLayout>(R.id.inputLayoutEmail)
        val emailEt = container.findViewById<TextInputEditText>(R.id.editEmail)
        val passLayout = container.findViewById<TextInputLayout>(R.id.inputLayoutPassword)
        val passEt = container.findViewById<TextInputEditText>(R.id.editPassword)

        emailEt.setText(auth.currentUser?.email ?: "")

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Change Email")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newEmail = emailEt.text?.toString()?.trim().orEmpty()
                val password = passEt.text?.toString().orEmpty()

                if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                    Toast.makeText(ctx, "Please enter a valid email", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val user = auth.currentUser
                if (user == null || user.email.isNullOrBlank()) {
                    Toast.makeText(ctx, "Not signed in", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // Re-auth then update email
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).addOnSuccessListener {
                    user.updateEmail(newEmail).addOnSuccessListener {
                        emailTv.text = newEmail
                        Toast.makeText(ctx, "Email updated", Toast.LENGTH_LONG).show()
                        // Optional: send verification
                        user.sendEmailVerification()
                    }.addOnFailureListener { e ->
                        Toast.makeText(ctx, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(ctx, "Re-auth failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }
}

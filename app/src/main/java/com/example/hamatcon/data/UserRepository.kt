package com.example.hamatcon.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object UserRepository {
    fun ensureUserDocument() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val docRef = Firebase.firestore.collection("users").document(user.uid)
        val data = mapOf(
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data, SetOptions.merge())
    }
}

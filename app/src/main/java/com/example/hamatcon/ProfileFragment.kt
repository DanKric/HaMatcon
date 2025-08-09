package com.example.hamatcon.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.example.hamatcon.LoginActivity
import com.example.hamatcon.R
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailTv = view.findViewById<TextView>(R.id.tvEmail)
        val nameTv = view.findViewById<TextView>(R.id.tvName)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)

        val user = FirebaseAuth.getInstance().currentUser
        emailTv.text = user?.email ?: ""
        nameTv.text = user?.displayName ?: ""

        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val i = Intent(requireContext(), LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            requireActivity().finish()
        }
    }
}

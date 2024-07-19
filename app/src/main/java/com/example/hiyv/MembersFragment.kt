package com.example.hiyv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.hiyv.databinding.DialogInviteMemberBinding
import com.example.hiyv.databinding.FragmentMembersBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addMemberIcon.setOnClickListener {
            showInviteMemberDialog()
        }
    }

    private fun showInviteMemberDialog() {
        val dialogBinding = DialogInviteMemberBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Invite Member")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnInvite.setOnClickListener {
            val email = dialogBinding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get the current user's ID
            val currentUserId = Firebase.auth.currentUser?.uid ?: ""
            if (currentUserId.isEmpty()) {
                Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            // Check if the email exists in the database
            val db = Firebase.firestore
            val userCollection = db.collection("users")

            userCollection.whereEqualTo("email", email).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(context, "Account does not exist", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        // Assuming there's only one document for each email
                        val userDoc = querySnapshot.documents[0]
                        val userId = userDoc.id

                        // Check if the inviter's user ID is already in the invites array
                        userDoc.reference.get()
                            .addOnSuccessListener { document ->
                                val invites = document.get("invites") as? List<String> ?: emptyList()

                                if (invites.contains(currentUserId)) {
                                    Toast.makeText(context, "You have already invited this user", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
                                    // Update the invites array
                                    userDoc.reference.update("invites", FieldValue.arrayUnion(currentUserId))
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Invite sent successfully", Toast.LENGTH_SHORT).show()
                                            dialog.dismiss()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Failed to send invite: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error checking invites: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error checking email: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

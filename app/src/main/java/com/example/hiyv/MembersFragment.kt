package com.example.hiyv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hiyv.databinding.DialogInviteMemberBinding
import com.example.hiyv.databinding.FragmentMembersBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    private var accountType: String? = null
    private var arrayOfMembers = mutableListOf<Member>()
    private lateinit var memberListAdapter: MemberListAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        Glide.with(this).load(R.drawable.members_backdrop).into(binding.gifContainer)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        memberListAdapter = MemberListAdapter(arrayOfMembers) { memberId ->
            removeMember(memberId)
        }
        binding.rvMemberList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMemberList.adapter = memberListAdapter

        binding.addMemberIcon.setOnClickListener {
            showInviteMemberDialog()
        }

        loadFamilyMembers()
    }

    private fun loadFamilyMembers() {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        listenerRegistration = db.collection("users").document(currentUserId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load family members: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val family = documentSnapshot.get("family") as? List<String> ?: emptyList()
                    arrayOfMembers.clear()

                    family.forEach { memberId ->
                        db.collection("users").document(memberId).get()
                            .addOnSuccessListener { memberDoc ->
                                val name = memberDoc.getString("name") ?: ""
                                val role = memberDoc.getString("accountType") ?: ""
                                val member = Member(memberId, name, role)
                                arrayOfMembers.add(member)
                                memberListAdapter.notifyDataSetChanged()
                            }
                    }
                }
            }
    }

    private fun removeMember(memberId: String) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("users").document(currentUserId)
            .update("family", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener {
                arrayOfMembers.removeAll { it.id == memberId }
                memberListAdapter.notifyDataSetChanged()
                Toast.makeText(context, "Member removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to remove member: ${e.message}", Toast.LENGTH_SHORT).show()
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

            val currentUserId = Firebase.auth.currentUser?.uid ?: ""
            if (currentUserId.isEmpty()) {
                Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            val db = Firebase.firestore
            val userCollection = db.collection("users")

            userCollection.whereEqualTo("email", email).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        Toast.makeText(context, "Account does not exist", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        val userDoc = querySnapshot.documents[0]
                        val userId = userDoc.id

                        userDoc.reference.get()
                            .addOnSuccessListener { document ->
                                val invites = document.get("invites") as? List<String> ?: emptyList()

                                if (invites.contains(currentUserId)) {
                                    Toast.makeText(context, "You have already invited this user", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
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
        listenerRegistration?.remove()
    }
}
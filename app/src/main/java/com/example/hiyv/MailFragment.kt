package com.example.hiyv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hiyv.databinding.FragmentMailBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MailFragment : Fragment() {

    private var _binding: FragmentMailBinding? = null
    private val binding get() = _binding!!
    private var accountType: String? = null
    private var arrayOfInvites = mutableListOf<Invite>()
    private lateinit var inviteListAdapter: InviteListAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            accountType = it.getString("ACCOUNT_TYPE")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inviteListAdapter = InviteListAdapter(arrayOfInvites,
            onAccept = { userId -> acceptInvite(userId) },
            onDecline = { userId -> declineInvite(userId) }
        )
        binding.rvInviteList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInviteList.adapter = inviteListAdapter

        loadInvites()
    }

    private fun loadInvites() {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        listenerRegistration = db.collection("users").document(currentUserId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load invites: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val invites = documentSnapshot.get("invites") as? List<String> ?: emptyList()
                    arrayOfInvites.clear()

                    invites.forEach { inviteId ->
                        db.collection("users").document(inviteId).get()
                            .addOnSuccessListener { inviteDoc ->
                                val name = inviteDoc.getString("Name") ?: ""
                                val invite = Invite(inviteId, name)
                                arrayOfInvites.add(invite)
                                inviteListAdapter.notifyDataSetChanged()
                            }
                    }
                }
            }
    }

    private fun acceptInvite(userId: String) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("users").document(currentUserId)
            .update("family", FieldValue.arrayUnion(userId), "invites", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                val invite = arrayOfInvites.find { it.userId == userId }
                arrayOfInvites.remove(invite)
                inviteListAdapter.notifyDataSetChanged()

                Toast.makeText(context, "Successfully joined Family!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to accept invite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun declineInvite(userId: String) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("users").document(currentUserId)
            .update("invites", FieldValue.arrayRemove(userId))
            .addOnSuccessListener {
                val invite = arrayOfInvites.find { it.userId == userId }
                arrayOfInvites.remove(invite)
                inviteListAdapter.notifyDataSetChanged()

                Toast.makeText(context, "Invite declined successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to decline invite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        listenerRegistration?.remove()
    }
}

package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hiyv.databinding.RowInviteBinding

class InviteListAdapter(
    private var invites: List<Invite>,
    private val onAccept: (String) -> Unit,
    private val onDecline: (String) -> Unit
) : RecyclerView.Adapter<InviteListAdapter.InviteViewHolder>() {

    inner class InviteViewHolder(val binding: RowInviteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(invite: Invite) {
            binding.tvName.text = invite.name
            binding.acceptInvite.setOnClickListener {
                onAccept(invite.userId)
            }
            binding.declineInvite.setOnClickListener {
                onDecline(invite.userId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val binding = RowInviteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InviteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        holder.bind(invites[position])
    }

    override fun getItemCount() = invites.size
}

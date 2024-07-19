package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemberListAdapter(
    private val members: List<Member>,
    private val onRemoveMember: (String) -> Unit
) : RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>() {

    class MemberViewHolder(val row: View, val onRemoveMember: (String) -> Unit) : RecyclerView.ViewHolder(row) {
        val tvName: TextView = row.findViewById(R.id.tvName)
        val tvRole: TextView = row.findViewById(R.id.tvRole)
        val removeButton: ImageButton = row.findViewById(R.id.remove_member)

        fun bind(member: Member) {
            tvName.text = member.name
            tvRole.text = member.role
            removeButton.setOnClickListener {
                onRemoveMember(member.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.row_member, parent, false)
        return MemberViewHolder(layout, onRemoveMember)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }
}

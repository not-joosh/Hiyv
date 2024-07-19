package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MemberListAdapter (private val members: Array<Member>)
    : RecyclerView.Adapter<MemberListAdapter.MemberViewHolder>(){

    class MemberViewHolder(val row: View): RecyclerView.ViewHolder(row) {
        val tvName: TextView = row.findViewById(R.id.tvName)
        val tvRole: TextView = row.findViewById(R.id.tvRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            MemberViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_member, parent, false)
        return MemberViewHolder(layout)
    }

    override fun getItemCount(): Int = members.size

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.tvName.text = members[position].name
        holder.tvRole.text = members[position].role
    }

}
package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScheduledTasksListAdapter (private val scheduledTasks: Array<ScheduledTask>)
    : RecyclerView.Adapter<ScheduledTasksListAdapter.ScheduledTasksViewHolder>(){

    class ScheduledTasksViewHolder(val row: View): RecyclerView.ViewHolder(row) {
        val tvSu: TextView = row.findViewById(R.id.tvSu)
        val tvM: TextView = row.findViewById(R.id.tvM)
        val tvT: TextView = row.findViewById(R.id.tvT)
        val tvW: TextView = row.findViewById(R.id.tvW)
        val tvTh: TextView = row.findViewById(R.id.tvTh)
        val tvF: TextView = row.findViewById(R.id.tvF)
        val tvS: TextView = row.findViewById(R.id.tvS)
        val tvTaskName: TextView = row.findViewById(R.id.tvTaskName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ScheduledTasksViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_scheduled_task, parent, false)
        return ScheduledTasksViewHolder(layout)
    }

    override fun getItemCount(): Int = scheduledTasks.size

    override fun onBindViewHolder(holder: ScheduledTasksViewHolder, position: Int) {
        if(scheduledTasks[position].isSunday) holder.tvSu.alpha = 1.0F
        else holder.tvSu.alpha = 0.1F
        if(scheduledTasks[position].isMonday) holder.tvM.alpha = 1.0F
        else holder.tvM.alpha = 0.1F
        if(scheduledTasks[position].isTuesday) holder.tvT.alpha = 1.0F
        else holder.tvT.alpha = 0.1F
        if(scheduledTasks[position].isWednesday) holder.tvW.alpha = 1.0F
        else holder.tvW.alpha = 0.1F
        if(scheduledTasks[position].isThursday) holder.tvTh.alpha = 1.0F
        else holder.tvTh.alpha = 0.1F
        if(scheduledTasks[position].isFriday) holder.tvF.alpha = 1.0F
        else holder.tvF.alpha = 0.1F
        if(scheduledTasks[position].isSaturday) holder.tvS.alpha = 1.0F
        else holder.tvS.alpha = 0.1F
        holder.tvTaskName.text = scheduledTasks[position].taskName
    }
}
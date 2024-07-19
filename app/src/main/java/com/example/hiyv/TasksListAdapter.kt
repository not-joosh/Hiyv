package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TasksListAdapter(
    private val tasks: Array<Task>,
    private val accountType: String
) : RecyclerView.Adapter<TasksListAdapter.TasksViewHolder>() {

    class TasksViewHolder(val row: View) : RecyclerView.ViewHolder(row) {
        val tvDate: TextView = row.findViewById(R.id.tvDate)
        val tvDateStr: TextView = row.findViewById(R.id.tvDateStr)
        val tvTaskName: TextView = row.findViewById(R.id.tvTaskName)
        val tvCompletedBy: TextView = row.findViewById(R.id.tvCompletedBy)
        val isCompleted: CheckBox = row.findViewById(R.id.checkBox)
        val removeTaskBtn: ImageButton = row.findViewById(R.id.removeTaskBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_task, parent, false)
        return TasksViewHolder(layout)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvDate.text = task.date
        holder.tvDateStr.text = task.dateStr
        holder.tvTaskName.text = task.taskName
        holder.tvCompletedBy.text = task.completedBy
        holder.isCompleted.isChecked = task.isCompleted

        // Hide removeTaskBtn if the accountType is "Child"
        if (accountType == "Child") {
            holder.removeTaskBtn.visibility = View.GONE
        } else {
            holder.removeTaskBtn.visibility = View.VISIBLE
        }

        // Update task status when CheckBox is clicked
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            val updatedTask = task.copy(isCompleted = isChecked)
            updateTaskInFirestore(updatedTask)
        }

        // Update task's completedBy field when the task is marked as completed
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val updatedTask = task.copy(
                isCompleted = isChecked,
                completedBy = if (isChecked) userId else ""
            )
            updateTaskInFirestore(updatedTask)
        }
    }

    private fun updateTaskInFirestore(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("tasks", FieldValue.arrayRemove(task)) // Remove old task
                .addOnSuccessListener {
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("tasks", FieldValue.arrayUnion(task)) // Add updated task
                        .addOnSuccessListener {
                            // Optional: Notify that the task was updated successfully
                        }
                        .addOnFailureListener {
                            // Optional: Notify that the task update failed
                        }
                }
                .addOnFailureListener {
                    // Optional: Notify that the task removal failed
                }
        }
    }
}
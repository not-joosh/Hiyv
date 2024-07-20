package com.example.hiyv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

        // Ensure the checkbox reflects the task's completion status
        holder.isCompleted.isChecked = task.isCompleted

        // If the checkbox's state is being updated, handle the state change accordingly
        holder.isCompleted.setOnCheckedChangeListener { _, isChecked ->
            // Avoid updating Firestore if the checkbox's state hasn't changed
            if (task.isCompleted != isChecked) {
                val updatedTask = task.copy(isCompleted = isChecked)
                updateTaskInFirestore(holder, updatedTask)
            }
        }

        if (accountType == "Child") {
            holder.removeTaskBtn.visibility = View.GONE
        } else {
            holder.removeTaskBtn.visibility = View.VISIBLE
            holder.removeTaskBtn.setOnClickListener {
                removeTaskFromFirestore(holder, task)
            }
        }
    }

    private fun updateTaskInFirestore(holder: TasksViewHolder, task: Task) {
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (accountType == "Child") {
            // For child accounts, update the task in the parent's document
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { childDocument ->
                    val myJoinedFamilies = childDocument.get("myJoinedFamilies") as? List<String> ?: return@addOnSuccessListener
                    if (myJoinedFamilies.isNotEmpty()) {
                        val parentId = myJoinedFamilies[0] // Assuming there's only one parent
                        firestore.collection("users").document(parentId)
                            .get()
                            .addOnSuccessListener { parentDocument ->
                                val tasks = parentDocument.get("tasks") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                                val updatedTasks = tasks.map { taskMap ->
                                    val taskName = taskMap["taskName"] as? String ?: return@map taskMap
                                    if (taskName == task.taskName) {
                                        // Replace the matching task
                                        mapOf(
                                            "date" to task.date,
                                            "dateStr" to task.dateStr,
                                            "taskName" to task.taskName,
                                            "completedBy" to task.completedBy,
                                            "isCompleted" to task.isCompleted
                                        )
                                    } else {
                                        taskMap
                                    }
                                }
                                firestore.collection("users").document(parentId)
                                    .update("tasks", updatedTasks)
                                    .addOnSuccessListener {
                                        Toast.makeText(holder.itemView.context, "Task updated successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(holder.itemView.context, "Failed to update task", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(holder.itemView.context, "Failed to fetch parent's tasks", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(holder.itemView.context, "No parent found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Failed to fetch child document", Toast.LENGTH_SHORT).show()
                }
        } else {
            // For parent accounts, update the task in their own document
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val tasks = document.get("tasks") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                    val updatedTasks = tasks.map { taskMap ->
                        val taskName = taskMap["taskName"] as? String ?: return@map taskMap
                        if (taskName == task.taskName) {
                            // Replace the matching task
                            mapOf(
                                "date" to task.date,
                                "dateStr" to task.dateStr,
                                "taskName" to task.taskName,
                                "completedBy" to task.completedBy,
                                "isCompleted" to task.isCompleted
                            )
                        } else {
                            taskMap
                        }
                    }
                    firestore.collection("users").document(userId)
                        .update("tasks", updatedTasks)
                        .addOnSuccessListener {
                            Toast.makeText(holder.itemView.context, "Task updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(holder.itemView.context, "Failed to update task", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Failed to fetch tasks", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun removeTaskFromFirestore(holder: TasksViewHolder, task: Task) {
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // For parent accounts, remove the task from their own document
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val tasks = document.get("tasks") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                val updatedTasks = tasks.filterNot { taskMap ->
                    val taskName = taskMap["taskName"] as? String ?: return@filterNot false
                    taskName == task.taskName // Compare with the taskName from the parameter
                }

                firestore.collection("users").document(userId)
                    .update("tasks", updatedTasks)
                    .addOnSuccessListener {
                        // Update the local task list and notify the adapter
                        // Find the position of the task to be removed
                        val position = tasks.indexOfFirst { (it["taskName"] as? String) == task.taskName }
                        if (position != -1) {
                            (tasks as MutableList).removeAt(position)
                            notifyItemRemoved(position)
                        }

                        Toast.makeText(holder.itemView.context, "Task removed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Failed to remove task", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Failed to fetch tasks", Toast.LENGTH_SHORT).show()
            }
    }
}

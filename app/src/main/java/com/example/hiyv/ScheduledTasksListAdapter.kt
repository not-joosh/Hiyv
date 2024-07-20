package com.example.hiyv

import android.icu.util.Calendar
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val task = scheduledTasks[position]
        val today = getToday() // Get current day index (0=Sunday, 1=Monday, ..., 6=Saturday)

//        if (shouldCreateTask(task, today)) {
//            createTaskFromScheduledTask(task)
//        }

        holder.tvSu.alpha = if (task.sunday) 1.0F else 0.1F
        holder.tvM.alpha = if (task.monday) 1.0F else 0.1F
        holder.tvT.alpha = if (task.tuesday) 1.0F else 0.1F
        holder.tvW.alpha = if (task.wednesday) 1.0F else 0.1F
        holder.tvTh.alpha = if (task.thursday) 1.0F else 0.1F
        holder.tvF.alpha = if (task.friday) 1.0F else 0.1F
        holder.tvS.alpha = if (task.saturday) 1.0F else 0.1F
        holder.tvTaskName.text = task.taskName

        // Set up delete button click listener
        holder.itemView.findViewById<ImageButton>(R.id.remove_scheduled_task).setOnClickListener {
            removeScheduledTask(position)
        }
    }
    private fun getToday(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK) - 1
    }

    private fun shouldCreateTask(task: ScheduledTask, today: Int): Boolean {
        // Check if today matches the scheduled day and if the task has not been created this week
        return today in task.getDaysOfWeek() && (task.lastCreated == null || !isSameWeek(task.lastCreated))
    }

    private fun isSameWeek(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        val taskWeek = calendar.get(Calendar.WEEK_OF_YEAR)
        val taskYear = calendar.get(Calendar.YEAR)

        return currentWeek == taskWeek && currentYear == taskYear
    }

    private fun createTaskFromScheduledTask(task: ScheduledTask) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = db.collection("users").document(userId)

        val newTask = mapOf(
            "date" to getCurrentDate(),
            "taskName" to task.taskName,
            "completedBy" to "",
            "isCompleted" to false
        )

        // Update tasks and scheduled_tasks in one transaction
        userDocRef.update("tasks", FieldValue.arrayUnion(newTask))
            .addOnSuccessListener {
                // Update lastCreated timestamp in the same update operation
                val updatedTask = task.copy(lastCreated = System.currentTimeMillis())
                userDocRef.update("scheduled_tasks", FieldValue.arrayRemove(task))
                    .addOnSuccessListener {
                        userDocRef.update("scheduled_tasks", FieldValue.arrayUnion(updatedTask))
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ScheduledTasksListAdapter", "Error creating task", e)
            }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun ScheduledTask.getDaysOfWeek(): List<Int> {
        return mutableListOf<Int>().apply {
            if (sunday) add(0)
            if (monday) add(1)
            if (tuesday) add(2)
            if (wednesday) add(3)
            if (thursday) add(4)
            if (friday) add(5)
            if (saturday) add(6)
        }
    }

    private fun removeScheduledTask(position: Int) {
        val taskToRemove = scheduledTasks[position]

        // Assuming you have a reference to the Firestore database
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Get the user's document reference
        val userDocRef = db.collection("users").document(userId)

        // Perform the task removal
        userDocRef.update("scheduled_tasks", FieldValue.arrayRemove(taskToRemove))
            .addOnSuccessListener {
                // Task removed successfully
                notifyItemRemoved(position)
            }
            .addOnFailureListener { e ->
                // Handle error
                Log.e("ScheduledTasksListAdapter", "Error removing task", e)
            }
    }
}
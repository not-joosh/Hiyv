package com.example.myapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hiyv.R
import com.example.hiyv.ScheduledTask
import com.example.hiyv.ScheduledTasksListAdapter
import com.example.hiyv.Task
import com.example.hiyv.TasksListAdapter
import com.example.hiyv.databinding.FragmentTasksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TasksFragment : Fragment() {

    private lateinit var binding: FragmentTasksBinding
    private var accountType: String? = null

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTasksBinding.inflate(inflater, container, false)

        // Retrieve account type from arguments
        accountType = arguments?.getString("ACCOUNT_TYPE") // Adjusted key to match your old code

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.taskListRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.scheduledTasksRecyclerView.layoutManager = LinearLayoutManager(context)

        loadTasks()
        loadScheduledTasks()  // Load scheduled tasks

        binding.addTaskIcon.setOnClickListener {
            showCreateTaskDialog()
        }

        binding.addScheduledTaskIcon.setOnClickListener {
            showCreateScheduledTaskDialog()
        }

        if (accountType == "Child") {
            binding.scheduledTasksRecyclerView.visibility = View.GONE
            binding.addScheduledTaskIcon.visibility = View.GONE
            binding.tvScheduleTask.visibility = View.GONE
            binding.tvCreateTask.visibility = View.GONE
            binding.addTaskIcon.visibility = View.GONE
        } else {
            binding.scheduledTasksRecyclerView.visibility = View.VISIBLE
            binding.addScheduledTaskIcon.visibility = View.VISIBLE
            binding.tvScheduleTask.visibility = View.VISIBLE
            binding.tvCreateTask.visibility = View.VISIBLE
            binding.addTaskIcon.visibility = View.VISIBLE
        }
    }

    private fun loadTasks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        if (accountType == "Child") {
            firestore.collection("users").document(userId)
                .addSnapshotListener { documentSnapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val myJoinedFamilies = documentSnapshot?.get("myJoinedFamilies") as? List<String> ?: return@addSnapshotListener
                    if (myJoinedFamilies.isNotEmpty()) {
                        val parentId = myJoinedFamilies[0] // Assuming there's only one parent
                        firestore.collection("users").document(parentId)
                            .addSnapshotListener { parentDocument, e ->
                                if (e != null) {
                                    Toast.makeText(context, "Failed to fetch parent's tasks", Toast.LENGTH_SHORT).show()
                                    return@addSnapshotListener
                                }

                                val tasks = parentDocument?.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                                val taskObjects = tasks.mapNotNull {
                                    val date = it["date"] as? String ?: return@mapNotNull null
                                    val dateStr = it["dateStr"] as? String ?: return@mapNotNull null
                                    val taskName = it["taskName"] as? String ?: return@mapNotNull null
                                    val completedBy = it["completedBy"] as? String ?: ""
                                    val isCompleted = it["isCompleted"] as? Boolean ?: false
                                    Task(date, dateStr, taskName, completedBy, isCompleted)
                                }
                                updateTaskList(taskObjects)
                            }
                    } else {
                        Toast.makeText(context, "No parent found", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            firestore.collection("users").document(userId)
                .addSnapshotListener { documentSnapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Failed to load tasks", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    val tasks = documentSnapshot?.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                    val taskObjects = tasks.mapNotNull {
                        val date = it["date"] as? String ?: return@mapNotNull null
                        val dateStr = it["dateStr"] as? String ?: return@mapNotNull null
                        val taskName = it["taskName"] as? String ?: return@mapNotNull null
                        val completedBy = it["completedBy"] as? String ?: ""
                        val isCompleted = it["isCompleted"] as? Boolean ?: false
                        Task(date, dateStr, taskName, completedBy, isCompleted)
                    }
                    updateTaskList(taskObjects)
                }
        }
    }

    private fun loadScheduledTasks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Failed to load scheduled tasks", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val scheduledTasks = documentSnapshot?.get("scheduled_tasks") as? List<Map<String, Any>> ?: emptyList()
                val scheduledTaskObjects = scheduledTasks.mapNotNull {
                    val taskName = it["taskName"] as? String ?: return@mapNotNull null
                    val monday = it["monday"] as? Boolean ?: false
                    val tuesday = it["tuesday"] as? Boolean ?: false
                    val wednesday = it["wednesday"] as? Boolean ?: false
                    val thursday = it["thursday"] as? Boolean ?: false
                    val friday = it["friday"] as? Boolean ?: false
                    val saturday = it["saturday"] as? Boolean ?: false
                    val sunday = it["sunday"] as? Boolean ?: false
                    ScheduledTask(taskName, monday, tuesday, wednesday, thursday, friday, saturday, sunday)
                }
                updateScheduledTaskList(scheduledTaskObjects)
            }
    }

    private fun updateTaskList(tasks: List<Task>) {
        val accountTypeNonNull = accountType ?: run {
            Toast.makeText(context, "Account type is missing", Toast.LENGTH_SHORT).show()
            return
        }
        val taskListAdapter = TasksListAdapter(tasks.toTypedArray(), accountTypeNonNull)
        binding.taskListRecyclerView.adapter = taskListAdapter
    }

    private fun updateScheduledTaskList(scheduledTasks: List<ScheduledTask>) {
        val scheduledTasksAdapter = ScheduledTasksListAdapter(scheduledTasks.toTypedArray())
        binding.scheduledTasksRecyclerView.adapter = scheduledTasksAdapter
    }


    private fun showCreateScheduledTaskDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_schedule_task, null)
        val taskNameEditText = dialogView.findViewById<EditText>(R.id.et_task_name_schedule)
        val daysLayout = dialogView.findViewById<LinearLayout>(R.id.ll_days)
        val checkAllCheckbox = dialogView.findViewById<CheckBox>(R.id.cb_check_all)

        // Add checkboxes for each day
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        for (day in days) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = day
            daysLayout.addView(checkBox)
        }
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Schedule Task")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                val taskName = taskNameEditText.text.toString()
                val selectedDays = mutableListOf<String>()
                for (i in 0 until daysLayout.childCount) {
                    val checkBox = daysLayout.getChildAt(i) as CheckBox
                    if (checkBox.isChecked) {
                        selectedDays.add(checkBox.text.toString())
                    }
                }
                if (checkAllCheckbox.isChecked) {
                    selectedDays.add("All Days")
                }

                if (taskName.isBlank() || selectedDays.isEmpty()) {
                    Toast.makeText(context, "Please enter a task name and select at least one day", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val scheduledTask = ScheduledTask(
                    taskName = taskName,
                    monday = selectedDays.contains("Monday"),
                    tuesday = selectedDays.contains("Tuesday"),
                    wednesday = selectedDays.contains("Wednesday"),
                    thursday = selectedDays.contains("Thursday"),
                    friday = selectedDays.contains("Friday"),
                    saturday = selectedDays.contains("Saturday"),
                    sunday = selectedDays.contains("Sunday"),
                    lastCreated = null // Set to null for the first creation
                )

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("scheduled_tasks", FieldValue.arrayUnion(scheduledTask))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Scheduled task created successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to create scheduled task", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        checkAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            for (i in 0 until daysLayout.childCount) {
                val checkBox = daysLayout.getChildAt(i) as CheckBox
                checkBox.isChecked = isChecked
            }
        }

        dialog.show()
    }


    private fun showCreateTaskDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_task, null)
        val taskNameEditText = dialogView.findViewById<EditText>(R.id.et_task_name)
        val selectDateButton = dialogView.findViewById<Button>(R.id.btn_select_date)
        val selectedDateTextView = dialogView.findViewById<TextView>(R.id.tv_selected_date)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New Task")
            .setView(dialogView)
            .setPositiveButton("Create") { dialog, _ ->
                val taskName = taskNameEditText.text.toString()
                val dueDate = selectedDateTextView.text.toString()

                if (taskName.isBlank() || dueDate.isBlank()) {
                    Toast.makeText(context, "Please enter a task name and select a due date", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val task = Task(
                    date = dueDate,
                    dateStr = dueDate,
                    taskName = taskName,
                    completedBy = "",
                    isCompleted = false
                )

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("tasks", FieldValue.arrayUnion(task))
                        .addOnSuccessListener {
                            Toast.makeText(context, "Task created successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to create task", Toast.LENGTH_SHORT).show()
                        }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        selectDateButton.setOnClickListener {
            showDatePicker { selectedDate ->
                selectedDateTextView.text = selectedDate
            }
        }

        dialog.show()
    }

}

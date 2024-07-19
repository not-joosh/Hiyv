package com.example.hiyv

import ScheduledTask
import Task
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.hiyv.databinding.FragmentTasksBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TasksFragment : Fragment() {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private var accountType: String? = null

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
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        Glide.with(this).load(R.drawable.task_backdrop).into(binding.gifContainer)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addTaskIcon.setOnClickListener {
            showCreateTaskDialog()
        }

        binding.addScheduledTaskIcon.setOnClickListener {
            showScheduleTaskDialog()
        }
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

    private fun showScheduleTaskDialog() {
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
                    isMonday = selectedDays.contains("Monday"),
                    isTuesday = selectedDays.contains("Tuesday"),
                    isWednesday = selectedDays.contains("Wednesday"),
                    isThursday = selectedDays.contains("Thursday"),
                    isFriday = selectedDays.contains("Friday"),
                    isSaturday = selectedDays.contains("Saturday"),
                    isSunday = selectedDays.contains("Sunday")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package com.example.hiyv

data class Task(
    val id: Long,
    val name: String,
    val dueDate: String,
    val assignedTo: String,
    var isCompleted: Boolean
)

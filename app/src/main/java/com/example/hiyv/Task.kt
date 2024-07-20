package  com.example.hiyv

data class Task(
    val date: String = "",
    val dateStr: String = "",
    val taskName: String = "",
    var completedBy: String = "",
    var isCompleted: Boolean = false
)

data class ScheduledTask(
    val taskName: String,
    val sunday: Boolean,
    val monday: Boolean,
    val tuesday: Boolean,
    val wednesday: Boolean,
    val thursday: Boolean,
    val friday: Boolean,
    val saturday: Boolean,
    val lastCreated: Long? = null // Timestamp of the last creation
)

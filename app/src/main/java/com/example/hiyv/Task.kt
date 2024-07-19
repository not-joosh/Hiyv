data class Task(
    val date: String = "",
    val dateStr: String = "",
    val taskName: String = "",
    val completedBy: String = "",
    val isCompleted: Boolean = false
)

data class ScheduledTask(
    val taskName: String = "",
    val isMonday: Boolean = false,
    val isTuesday: Boolean = false,
    val isWednesday: Boolean = false,
    val isThursday: Boolean = false,
    val isFriday: Boolean = false,
    val isSaturday: Boolean = false,
    val isSunday: Boolean = false
)

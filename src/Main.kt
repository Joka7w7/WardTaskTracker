// Ward Task Tracker
// A console program that models a role-based task list for a ward leadership team.
// All names and tasks are FICTIONAL sample data.
// Author: Jose Chanax | CSE 310 - Module 1 (Kotlin)

import java.time.LocalDate

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/** The access levels in the app. Each role sees a different set of tasks. */
enum class Role(val label: String) {
    BISHOP("Bishop"),
    COUNSELOR("Counselor"),
    ORG_LEADER("Organization Leader")
}

/** A task assigned to an organization. A data class gives us copy(), equals() and toString() for free. */
data class Task(
    val id: Int,
    val title: String,
    val organization: String,
    val dueDate: LocalDate,
    val done: Boolean = false
)

/** A person using the app. "supervises" lists the organizations this person can see. */
data class Member(val name: String, val role: Role, val supervises: List<String>)

// ---------------------------------------------------------------------------
// Task manager (class that owns the list of tasks)
// ---------------------------------------------------------------------------

class TaskManager {
    // A mutable collection that we create and modify while the program runs.
    private val tasks = mutableListOf<Task>()
    private var nextId = 1 // 'var' because the counter changes every time we add a task

    /** Creates a new task, stores it, and returns it. */
    fun addTask(title: String, organization: String, daysFromToday: Long): Task {
        val task = Task(nextId, title, organization, LocalDate.now().plusDays(daysFromToday))
        tasks.add(task)
        nextId++
        return task
    }

    /** Marks a task as done. Data classes are immutable here, so we use copy() to build the updated task. */
    fun completeTask(id: Int): Boolean {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return false          // no task with that id
        tasks[index] = tasks[index].copy(done = true)
        return true
    }

    /** Returns only the tasks that this member is allowed to see, based on role. */
    fun tasksFor(member: Member): List<Task> {
        return when (member.role) {
            Role.BISHOP -> tasks.toList()                                   // sees everything
            Role.COUNSELOR, Role.ORG_LEADER ->
                tasks.filter { it.organization in member.supervises }      // sees only assigned organizations
        }
    }

    /** Checks whether a member may add a task to the given organization. */
    fun canAddTo(member: Member, organization: String): Boolean {
        return member.role == Role.BISHOP || organization in member.supervises
    }

    /** Calculates the completion percentage (0-100) for one organization. */
    fun progressFor(organization: String): Int {
        val orgTasks = tasks.filter { it.organization == organization }
        if (orgTasks.isEmpty()) return 0
        val finished = orgTasks.count { it.done }
        return finished * 100 / orgTasks.size
    }
}

// ---------------------------------------------------------------------------
// Helper functions
// ---------------------------------------------------------------------------

/** Prints one task on a single line, marking it as done, overdue or pending. */
fun printTask(task: Task) {
    val today = LocalDate.now()
    val status = when {
        task.done -> "DONE"
        task.dueDate.isBefore(today) -> "OVERDUE"
        task.dueDate == today -> "DUE TODAY"
        else -> "PENDING"
    }
    println("  #${task.id} [$status] ${task.title} (${task.organization}, due ${task.dueDate})")
}

/** Prints the main menu. */
fun printMenu(user: Member) {
    println("\n=== Ward Task Tracker | ${user.name} (${user.role.label}) ===")
    println("1. List my tasks")
    println("2. Add a task")
    println("3. Complete a task")
    println("4. Progress by organization")
    println("5. Switch user")
    println("0. Exit")
    print("Choose an option: ")
}

/** Keeps asking until the user types something that is not blank. */
fun readNonEmpty(prompt: String): String {
    while (true) {
        print(prompt)
        val text = readLine()?.trim() ?: return ""   // readLine() returns null if input ends
        if (text.isNotEmpty()) return text
        println("This field cannot be empty.")
    }
}

/** Lets the user pick one organization from a numbered list. Returns null if the choice is invalid. */
fun chooseOrganization(options: List<String>): String? {
    for ((index, name) in options.withIndex()) {
        println("  ${index + 1}. $name")
    }
    val choice = readNonEmpty("Organization number: ").toIntOrNull()
    return if (choice != null && choice in 1..options.size) options[choice - 1] else null
}

/** Lets the user pick one of the sample members. */
fun chooseUser(members: List<Member>): Member {
    println("\nWho is using the app?")
    for ((index, member) in members.withIndex()) {
        println("  ${index + 1}. ${member.name} - ${member.role.label}")
    }
    while (true) {
        val choice = readNonEmpty("Number: ").toIntOrNull()
        if (choice != null && choice in 1..members.size) return members[choice - 1]
        println("Please type a number from the list.")
    }
}

/** Loads fictional sample data so the demo has something to show. */
fun loadSampleTasks(manager: TaskManager) {
    manager.addTask("Plan the monthly activity", "Relief Society", 5)
    manager.addTask("Update the lesson schedule", "Sunday School", 2)
    manager.addTask("Confirm Thursday youth activity", "Young Men", -1)   // negative = already overdue
    manager.addTask("Prepare Primary program rehearsal", "Primary", 10)
    manager.addTask("Review the quarterly goals", "Elders Quorum", 7)
    manager.addTask("Order supplies for the youth camp", "Young Women", 14)
    manager.completeTask(4)   // mark one as already finished
}

// ---------------------------------------------------------------------------
// Program entry point
// ---------------------------------------------------------------------------

fun main() {
    val organizations = listOf(
        "Relief Society", "Elders Quorum", "Young Men", "Young Women", "Primary", "Sunday School"
    )
    // Fictional people with different access levels.
    val members = listOf(
        Member("Bishop Smith", Role.BISHOP, organizations),
        Member("Brother Jones", Role.COUNSELOR, listOf("Young Men", "Young Women", "Primary")),
        Member("Sister Lopez", Role.ORG_LEADER, listOf("Relief Society"))
    )

    val manager = TaskManager()
    loadSampleTasks(manager)

    var user = chooseUser(members)   // 'var' because the user can switch

    var running = true
    while (running) {
        printMenu(user)
        when (readLine()?.trim()) {
            "1" -> {
                val visible = manager.tasksFor(user)
                if (visible.isEmpty()) println("You have no tasks.")
                else for (task in visible) printTask(task)
            }
            "2" -> {
                val allowed = if (user.role == Role.BISHOP) organizations else user.supervises
                val org = chooseOrganization(allowed)
                if (org == null || !manager.canAddTo(user, org)) {
                    println("Invalid choice.")
                } else {
                    val title = readNonEmpty("Task title: ")
                    val days = readNonEmpty("Due in how many days? ").toLongOrNull()
                    if (days == null) {
                        println("Please enter a whole number.")
                    } else {
                        val created = manager.addTask(title, org, days)
                        println("Task created:")
                        printTask(created)
                    }
                }
            }
            "3" -> {
                val id = readNonEmpty("Task number to complete: ").toIntOrNull()
                val ownTaskIds = manager.tasksFor(user).map { it.id }   // users may only complete tasks they can see
                if (id != null && id in ownTaskIds && manager.completeTask(id)) {
                    println("Task #$id marked as done.")
                } else {
                    println("Task not found.")
                }
            }
            "4" -> {
                val shown = if (user.role == Role.BISHOP) organizations else user.supervises
                for (org in shown) {
                    println("  $org: ${manager.progressFor(org)}% complete")
                }
            }
            "5" -> user = chooseUser(members)
            "0", null -> running = false
            else -> println("Invalid option. Please choose from the menu.")
        }
    }
    println("Goodbye!")
}
// Ward Task Tracker - version 2
// Tracks action items (follow-ups) from ward meetings and attaches each one to a calling.
// Callings form a chain of command, from the bishop down to teachers and ministering brothers/sisters.
// Every item keeps a history log so leaders can see what was decided and who must follow up.
// All names and tasks are FICTIONAL sample data.
// Author: Jose Chanax | CSE 310 - Module 1 (Kotlin)

import java.time.LocalDate

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/** Where an action item came from. Useful for finding "what was discussed" later. */
enum class MeetingType(val label: String) {
    BISHOPRIC("Bishopric meeting"),
    WARD_COUNCIL("Ward council"),
    YOUTH_COUNCIL("Ward youth council"),
    MISSIONARY("Missionary coordination"),
    ONE_ON_ONE("One-on-one / other")
}

/** Whether an item is still open or already finished. */
enum class Status { OPEN, DONE }

/** A calling in the ward. "reportsTo" is the id of the calling above it (null for the bishop). */
data class Calling(
    val id: String,
    val title: String,
    val reportsTo: String?,
    val isBishopric: Boolean = false
)

/** A (fictional) person serving in one calling. */
data class Person(val name: String, val callingId: String)

/** One line in an item's history: who did what, and when. */
data class LogEntry(val date: LocalDate, val by: String, val note: String)

/** A follow-up assigned to a calling. The history list lets us "backtrack" every change. */
data class ActionItem(
    val id: Int,
    val title: String,
    val ownerCallingId: String,
    val source: MeetingType,
    val dueDate: LocalDate,
    val status: Status = Status.OPEN,
    val confidential: Boolean = false,       // e.g. proposed callings stay between the bishopric
    val history: List<LogEntry> = emptyList()
)

// ---------------------------------------------------------------------------
// Directory: the chain of command
// ---------------------------------------------------------------------------

class Directory(callings: List<Calling>, val people: List<Person>) {
    // Map for quick lookup by id.
    private val byId: Map<String, Calling> = callings.associateBy { it.id }

    /** Returns the title of a calling, or the id itself if unknown. */
    fun title(id: String): String = byId[id]?.title ?: id

    /** Returns this calling plus every calling below it (recursive). */
    fun subtree(id: String): Set<String> {
        val result = mutableSetOf(id)
        for (child in byId.values.filter { it.reportsTo == id }) {
            result.addAll(subtree(child.id))
        }
        return result
    }

    /** Returns the chain from this calling up to the bishop. */
    fun chainOfCommand(id: String): List<Calling> {
        val chain = mutableListOf<Calling>()
        var current = byId[id]
        while (current != null) {
            chain.add(current)
            current = current.reportsTo?.let { byId[it] }
        }
        return chain
    }

    /** Finds the person who holds a calling, if someone is assigned. */
    fun personIn(callingId: String): Person? = people.firstOrNull { it.callingId == callingId }

    /** True when the calling is the bishop or one of his counselors. */
    fun isBishopric(callingId: String): Boolean = byId[callingId]?.isBishopric == true
}

// ---------------------------------------------------------------------------
// Tracker: owns the list of action items
// ---------------------------------------------------------------------------

class Tracker(private val directory: Directory) {
    private val items = mutableListOf<ActionItem>()
    private var nextId = 1

    /** Adds an item. Returns null if the assigner may not give work to that calling. */
    fun addItem(
        by: Person, title: String, ownerCallingId: String,
        source: MeetingType, daysFromToday: Long, confidential: Boolean = false
    ): ActionItem? {
        // A leader can assign work only to himself/herself or to callings below.
        if (ownerCallingId !in directory.subtree(by.callingId)) return null
        val created = LogEntry(LocalDate.now(), by.name, "Created from: ${source.label}")
        val item = ActionItem(
            nextId, title, ownerCallingId, source,
            LocalDate.now().plusDays(daysFromToday), Status.OPEN, confidential, listOf(created)
        )
        items.add(item)
        nextId++
        return item
    }

    /** Returns the items this person may see, based on the chain of command. */
    fun visibleTo(viewer: Person): List<ActionItem> {
        val allowed = directory.subtree(viewer.callingId)
        return items.filter { item ->
            item.ownerCallingId in allowed &&
                    (!item.confidential ||
                            directory.isBishopric(viewer.callingId) ||
                            item.ownerCallingId == viewer.callingId)
        }
    }

    /** Finds a visible item by id (so people cannot open items they should not see). */
    fun findVisible(viewer: Person, id: Int): ActionItem? = visibleTo(viewer).firstOrNull { it.id == id }

    /** Adds a note to the history. Data classes are immutable, so we replace the item with a copy. */
    fun addNote(viewer: Person, id: Int, note: String): Boolean {
        val item = findVisible(viewer, id) ?: return false
        replace(item.copy(history = item.history + LogEntry(LocalDate.now(), viewer.name, note)))
        return true
    }

    /** Marks an item as done and records it in the history. */
    fun complete(viewer: Person, id: Int, note: String): Boolean {
        val item = findVisible(viewer, id) ?: return false
        val entry = LogEntry(LocalDate.now(), viewer.name, "Completed: $note")
        replace(item.copy(status = Status.DONE, history = item.history + entry))
        return true
    }

    /** Open items that are overdue or due within the next few days. */
    fun reminders(viewer: Person, withinDays: Long = 3): List<ActionItem> {
        val limit = LocalDate.now().plusDays(withinDays)
        return visibleTo(viewer).filter { it.status == Status.OPEN && !it.dueDate.isAfter(limit) }
    }

    /** Replaces an item in the list with an updated copy that has the same id. */
    private fun replace(updated: ActionItem) {
        val index = items.indexOfFirst { it.id == updated.id }
        if (index != -1) items[index] = updated
    }
}

// ---------------------------------------------------------------------------
// Display and input helpers
// ---------------------------------------------------------------------------

/** Prints one item on one line, with its status label. */
fun printItem(item: ActionItem, directory: Directory) {
    val today = LocalDate.now()
    val label = when {
        item.status == Status.DONE -> "DONE"
        item.dueDate.isBefore(today) -> "OVERDUE"
        item.dueDate == today -> "DUE TODAY"
        else -> "OPEN"
    }
    val secret = if (item.confidential) " [confidential]" else ""
    val who = directory.personIn(item.ownerCallingId)?.name ?: "(unassigned)"
    println("  #${item.id} [$label] ${item.title}$secret")
    println("      Owner: ${directory.title(item.ownerCallingId)} - $who | Due: ${item.dueDate} | From: ${item.source.label}")
}

/** Prints the full history of one item. */
fun printHistory(item: ActionItem) {
    println("History of #${item.id} - ${item.title}")
    for ((date, by, note) in item.history) {
        println("  $date | $by: $note")
    }
}

/** Prints the chain of command above a calling, e.g. "Primary Teacher > Primary President > ... > Bishop". */
fun printChain(person: Person, directory: Directory) {
    val chain = directory.chainOfCommand(person.callingId).joinToString(" > ") { it.title }
    println("Chain of command: $chain")
}

/** Prints the menu. */
fun printMenu(user: Person, directory: Directory) {
    println("\n=== ${user.name} - ${directory.title(user.callingId)} ===")
    println("1. My items and my team's items")
    println("2. Add an action item")
    println("3. Add a note to an item")
    println("4. Complete an item")
    println("5. View history of an item")
    println("6. Reminders (overdue or due soon)")
    println("7. Show my chain of command")
    println("8. Switch user")
    println("0. Exit")
    print("Choose an option: ")
}

/** Keeps asking until the user types something that is not blank. */
fun readNonEmpty(prompt: String): String {
    while (true) {
        print(prompt)
        val text = readlnOrNull()?.trim() ?: return ""
        if (text.isNotEmpty()) return text
        println("This field cannot be empty.")
    }
}

/** Lets the user pick a person from a numbered list. */
fun choosePerson(people: List<Person>, directory: Directory): Person {
    println("\nWho is using the app?")
    for ((index, p) in people.withIndex()) {
        println("  ${index + 1}. ${p.name} - ${directory.title(p.callingId)}")
    }
    while (true) {
        val choice = readNonEmpty("Number: ").toIntOrNull()
        if (choice != null && choice in 1..people.size) return people[choice - 1]
        println("Please type a number from the list.")
    }
}

/** Lets the user pick one of the callings below them (or their own) to receive an item. */
fun chooseOwner(user: Person, directory: Directory): String? {
    val ids = directory.subtree(user.callingId).toList()
    for ((index, id) in ids.withIndex()) {
        println("  ${index + 1}. ${directory.title(id)}")
    }
    val choice = readNonEmpty("Assign to number: ").toIntOrNull()
    return if (choice != null && choice in 1..ids.size) ids[choice - 1] else null
}

/** Lets the user pick the meeting where the item came from. */
fun chooseMeeting(): MeetingType {
    val options = MeetingType.entries
    for ((index, m) in options.withIndex()) println("  ${index + 1}. ${m.label}")
    val choice = readNonEmpty("Meeting number: ").toIntOrNull()
    return if (choice != null && choice in 1..options.size) options[choice - 1] else MeetingType.ONE_ON_ONE
}

// ---------------------------------------------------------------------------
// Sample data (fictional)
// ---------------------------------------------------------------------------

/** Builds the ward's chain of command. The bishop decides the real assignments; these are examples. */
fun buildDirectory(): Directory {
    val callings = listOf(
        Calling("bishop", "Bishop", null, true),
        Calling("c1", "First Counselor", "bishop", true),
        Calling("c2", "Second Counselor", "bishop", true),
        Calling("exec", "Ward Executive Secretary", "bishop"),
        Calling("clerk", "Ward Clerk", "bishop"),
        Calling("eq", "Elders Quorum President", "c1"),
        Calling("eq_c1", "Elders Quorum Counselor", "eq"),
        Calling("min_bro", "Ministering Brother", "eq"),
        Calling("rs", "Relief Society President", "c2"),
        Calling("rs_c1", "Relief Society Counselor", "rs"),
        Calling("min_sis", "Ministering Sister", "rs"),
        Calling("yw", "Young Women President", "c2"),
        Calling("primary", "Primary President", "c1"),
        Calling("primary_teacher", "Primary Teacher", "primary"),
        Calling("ss", "Sunday School President", "c1"),
        Calling("ss_teacher", "Sunday School Teacher", "ss"),
        Calling("mission", "Ward Mission Leader", "c2"),
        Calling("missionary", "Ward Missionary", "mission"),
        Calling("deacons", "Deacons Quorum President", "bishop")
    )
    val people = listOf(
        Person("Bishop Smith", "bishop"),
        Person("Brother Jones", "c1"),
        Person("Brother Garcia", "c2"),
        Person("Brother Perez", "exec"),
        Person("Brother Ramos", "eq"),
        Person("Sister Lopez", "rs"),
        Person("Sister Cruz", "primary"),
        Person("Sister Diaz", "primary_teacher"),
        Person("Brother Torres", "min_bro"),
        Person("Sister Mora", "yw")
    )
    return Directory(callings, people)
}

/** Loads a few example items and notes so the demo has history to show. */
fun loadSampleItems(tracker: Tracker, directory: Directory) {
    val bishop = directory.people.first { it.callingId == "bishop" }
    tracker.addItem(bishop, "Confirm the Primary program plan", "primary", MeetingType.WARD_COUNCIL, 5)
    tracker.addItem(bishop, "Schedule the quarterly ministering review", "bishop", MeetingType.ONE_ON_ONE, 20)
    tracker.addItem(bishop, "Confirm Thursday youth activity", "yw", MeetingType.YOUTH_COUNCIL, -1)
    tracker.addItem(bishop, "Prepare a proposed calling for review", "c1", MeetingType.BISHOPRIC, 3, true)
    tracker.addItem(bishop, "Visit the assigned family", "min_bro", MeetingType.WARD_COUNCIL, 7)
    tracker.addItem(bishop, "Report on the weekly coordination meeting", "mission", MeetingType.MISSIONARY, 2)
    val cruz = directory.people.first { it.callingId == "primary" }
    tracker.addNote(cruz, 1, "Teachers confirmed; waiting for the music leader")
}

// ---------------------------------------------------------------------------
// Program entry point
// ---------------------------------------------------------------------------

fun main() {
    val directory = buildDirectory()
    val tracker = Tracker(directory)
    loadSampleItems(tracker, directory)

    var user = choosePerson(directory.people, directory)
    println("You have ${tracker.reminders(user).size} reminder(s) today.")

    var running = true
    while (running) {
        printMenu(user, directory)
        when (readlnOrNull()?.trim()) {
            "1" -> {
                val list = tracker.visibleTo(user)
                if (list.isEmpty()) println("No items.") else list.forEach { printItem(it, directory) }
            }
            "2" -> {
                val owner = chooseOwner(user, directory)
                if (owner == null) {
                    println("Invalid choice.")
                } else {
                    val source = chooseMeeting()
                    val title = readNonEmpty("What needs to be done? ")
                    val days = readNonEmpty("Due in how many days? ").toLongOrNull()
                    val secret = readNonEmpty("Confidential? (y/n): ").lowercase() == "y"
                    if (days == null) {
                        println("Please enter a whole number.")
                    } else {
                        val created = tracker.addItem(user, title, owner, source, days, secret)
                        if (created != null) { println("Created:"); printItem(created, directory) }
                        else println("You cannot assign items to that calling.")
                    }
                }
            }
            "3" -> {
                val id = readNonEmpty("Item number: ").toIntOrNull()
                val note = readNonEmpty("Note: ")
                println(if (id != null && tracker.addNote(user, id, note)) "Note saved." else "Item not found.")
            }
            "4" -> {
                val id = readNonEmpty("Item number: ").toIntOrNull()
                val note = readNonEmpty("Result or comment: ")
                println(if (id != null && tracker.complete(user, id, note)) "Marked as done." else "Item not found.")
            }
            "5" -> {
                val id = readNonEmpty("Item number: ").toIntOrNull()
                val item = if (id != null) tracker.findVisible(user, id) else null
                if (item == null) println("Item not found.") else printHistory(item)
            }
            "6" -> {
                val list = tracker.reminders(user)
                if (list.isEmpty()) println("Nothing due soon.") else list.forEach { printItem(it, directory) }
            }
            "7" -> printChain(user, directory)
            "8" -> user = choosePerson(directory.people, directory)
            "0", null -> running = false
            else -> println("Invalid option. Please choose from the menu.")
        }
    }
    println("Goodbye!")
}
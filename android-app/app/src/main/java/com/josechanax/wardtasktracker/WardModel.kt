package com.josechanax.wardtasktracker

// WardModel.kt
// The data and rules of Ward Task Tracker. This is the logic from my Kotlin console
// program (Module 1), now without any console input or output so the Android screens can use it.
// Every action item belongs to a calling. Callings form a chain of command, and a leader can
// see and assign only the items of their own calling and the callings below it.
// All names and tasks are FICTIONAL sample data.

import java.time.LocalDate

/** Where an action item came from, so leaders can look back at what was discussed. */
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

/** A follow-up assigned to a calling. The history list lets us backtrack every change. */
data class ActionItem(
    val id: Int,
    val title: String,
    val ownerCallingId: String,
    val source: MeetingType,
    val dueDate: LocalDate,
    val status: Status = Status.OPEN,
    val confidential: Boolean = false,
    val history: List<LogEntry> = emptyList()
)

/** The chain of command: who reports to whom, and who holds each calling. */
class Directory(callings: List<Calling>, val people: List<Person>) {
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

/** Owns the list of action items and enforces who can see and assign what. */
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

    /** Finds a visible item by id, so people cannot open items they should not see. */
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

/** Returns a short label for an item: DONE, OVERDUE, DUE TODAY or OPEN. */
fun statusLabel(item: ActionItem): String {
    val today = LocalDate.now()
    return when {
        item.status == Status.DONE -> "DONE"
        item.dueDate.isBefore(today) -> "OVERDUE"
        item.dueDate == today -> "DUE TODAY"
        else -> "OPEN"
    }
}

/** Builds the ward's chain of command. The bishop decides the real assignments; these are examples. */
fun buildDirectory(): Directory {
    val callings = listOf(
        Calling("bishop", "Bishop", null, true),
        Calling("c1", "First Counselor", "bishop", true),
        Calling("c2", "Second Counselor", "bishop", true),
        Calling("exec", "Ward Executive Secretary", "bishop"),
        Calling("clerk", "Ward Clerk", "bishop"),
        Calling("eq", "Elders Quorum President", "c1"),
        Calling("min_bro", "Ministering Brother", "eq"),
        Calling("rs", "Relief Society President", "c2"),
        Calling("min_sis", "Ministering Sister", "rs"),
        Calling("yw", "Young Women President", "c2"),
        Calling("primary", "Primary President", "c1"),
        Calling("primary_teacher", "Primary Teacher", "primary"),
        Calling("ss", "Sunday School President", "c1"),
        Calling("ss_teacher", "Sunday School Teacher", "ss"),
        Calling("mission", "Ward Mission Leader", "c2"),
        Calling("missionary", "Ward Missionary", "mission")
    )
    val people = listOf(
        Person("Bishop Smith", "bishop"),
        Person("Brother Jones", "c1"),
        Person("Brother Garcia", "c2"),
        Person("Brother Ramos", "eq"),
        Person("Sister Lopez", "rs"),
        Person("Sister Cruz", "primary"),
        Person("Sister Diaz", "primary_teacher"),
        Person("Brother Torres", "min_bro")
    )
    return Directory(callings, people)
}

/** Loads a few fictional example items so the app has something to show. */
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
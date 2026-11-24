package com.josechanax.wardtasktracker

// MainActivity.kt
// The screens of Ward Task Tracker, built with Jetpack Compose.
// Screens: 1) Role Picker  2) Items List  3) Add Item form  4) Item Detail with history.
// The app uses the data and rules from WardModel.kt (the same logic as my Kotlin console program).
// All names and tasks are FICTIONAL sample data.

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

// ---------------------------------------------------------------------------
// App state
// ---------------------------------------------------------------------------

/** The screens of the app. Detail carries the id of the item being viewed. */
sealed class Screen {
    object Picker : Screen()
    object ItemsList : Screen()
    object AddItem : Screen()
    data class Detail(val itemId: Int) : Screen()
}

/** Holds the app's data and the current screen, so they survive screen rotation. */
class AppViewModel : ViewModel() {
    val directory: Directory = buildDirectory()
    val tracker: Tracker = Tracker(directory).also { loadSampleItems(it, directory) }

    // 'var' with mutableStateOf: when these change, the screens redraw automatically.
    var currentUser by mutableStateOf<Person?>(null)
    var screen by mutableStateOf<Screen>(Screen.Picker)

    // The tracker is a plain class, so we bump this counter after every change to refresh the lists.
    var version by mutableIntStateOf(0)

    /** Tells the screens that the data changed. */
    fun refresh() {
        version++
    }
}

/** The activity that hosts the whole app. */
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Starts the app: sets up the theme and shows the app content. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WardApp(vm)
                }
            }
        }
    }
}

/** Chooses which screen to show, and handles the phone's Back button. */
@Composable
fun WardApp(vm: AppViewModel) {
    val screen = vm.screen
    // Back button: Detail and Add go back to the list; the list goes back to the role picker.
    BackHandler(enabled = screen !is Screen.Picker) {
        vm.screen = if (screen is Screen.ItemsList) Screen.Picker else Screen.ItemsList
    }
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        when (screen) {
            is Screen.Picker -> PickerScreen(vm)
            is Screen.ItemsList -> ItemsListScreen(vm)
            is Screen.AddItem -> AddItemScreen(vm)
            is Screen.Detail -> DetailScreen(vm, screen.itemId)
        }
    }
}

// ---------------------------------------------------------------------------
// Screen 1: Role Picker
// ---------------------------------------------------------------------------

/** Lets the user choose which (fictional) person is using the app. */
@Composable
fun PickerScreen(vm: AppViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Ward Task Tracker", style = MaterialTheme.typography.headlineMedium)
            Text("Sample data only. Who is using the app?", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(vm.directory.people) { person ->
            Card(modifier = Modifier.fillMaxWidth().clickable {
                vm.currentUser = person
                vm.screen = Screen.ItemsList
            }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(person.name, fontWeight = FontWeight.Bold)
                    Text(vm.directory.title(person.callingId))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen 2: Items List
// ---------------------------------------------------------------------------

/** Shows the items this person may see, with reminders and their chain of command. */
@Composable
fun ItemsListScreen(vm: AppViewModel) {
    val user = vm.currentUser ?: return
    // Reading vm.version here makes the list redraw after every change.
    val visibleItems = remember(vm.version, user) { vm.tracker.visibleTo(user) }
    val reminderCount = remember(vm.version, user) { vm.tracker.reminders(user).size }
    val chain = vm.directory.chainOfCommand(user.callingId).joinToString(" > ") { it.title }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(user.name, style = MaterialTheme.typography.headlineSmall)
        Text(vm.directory.title(user.callingId), style = MaterialTheme.typography.titleMedium)
        Text("Chain of command: $chain", style = MaterialTheme.typography.bodySmall)
        Text("Reminders (overdue or due soon): $reminderCount", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.screen = Screen.AddItem }) { Text("Add item") }
            OutlinedButton(onClick = { vm.screen = Screen.Picker }) { Text("Switch user") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (visibleItems.isEmpty()) {
            Text("No items for this role.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleItems, key = { it.id }) { item ->
                    ItemCard(item, vm.directory) { vm.screen = Screen.Detail(item.id) }
                }
            }
        }
    }
}

/** One card in the list. The color shows the status: red = overdue, grey = done. */
@Composable
fun ItemCard(item: ActionItem, directory: Directory, onClick: () -> Unit) {
    val label = statusLabel(item)
    val color = when (label) {
        "OVERDUE" -> MaterialTheme.colorScheme.errorContainer
        "DONE" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("[$label] ${item.title}", fontWeight = FontWeight.Bold)
            val secret = if (item.confidential) " | confidential" else ""
            Text("Owner: ${directory.title(item.ownerCallingId)} | Due: ${item.dueDate}$secret")
            Text("From: ${item.source.label}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Screen 3: Add Item form
// ---------------------------------------------------------------------------

/** A simple drop-down: a button that opens a menu of options. */
@Composable
fun DropdownField(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(options[selectedIndex])
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(index)
                    expanded = false
                })
            }
        }
    }
}

/** A form to create a new action item for this role or a role below it. */
@Composable
fun AddItemScreen(vm: AppViewModel) {
    val user = vm.currentUser ?: return
    val ownerIds = remember(user) { vm.directory.subtree(user.callingId).toList() }
    val meetings = MeetingType.entries

    var title by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("7") }
    var ownerIndex by remember { mutableIntStateOf(0) }
    var meetingIndex by remember { mutableIntStateOf(0) }
    var confidential by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add an action item", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("What needs to be done?") }, modifier = Modifier.fillMaxWidth()
        )
        DropdownField("Assign to", ownerIds.map { vm.directory.title(it) }, ownerIndex) { ownerIndex = it }
        DropdownField("Came from", meetings.map { it.label }, meetingIndex) { meetingIndex = it }
        OutlinedTextField(
            value = days, onValueChange = { days = it },
            label = { Text("Due in how many days?") }, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(checked = confidential, onCheckedChange = { confidential = it })
            Text("Confidential")
        }
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val dueDays = days.trim().toLongOrNull()
                if (title.isBlank()) {
                    error = "Please write what needs to be done."
                } else if (dueDays == null) {
                    error = "Due days must be a whole number."
                } else {
                    val created = vm.tracker.addItem(
                        user, title.trim(), ownerIds[ownerIndex], meetings[meetingIndex], dueDays, confidential
                    )
                    if (created == null) {
                        error = "You cannot assign items to that role."
                    } else {
                        vm.refresh()
                        vm.screen = Screen.ItemsList
                    }
                }
            }) { Text("Save") }
            OutlinedButton(onClick = { vm.screen = Screen.ItemsList }) { Text("Cancel") }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen 4: Item Detail with history
// ---------------------------------------------------------------------------

/** Shows one item with its full history, and lets the user add a note or mark it as done. */
@Composable
fun DetailScreen(vm: AppViewModel, itemId: Int) {
    val user = vm.currentUser ?: return
    val item = remember(vm.version, itemId) { vm.tracker.findVisible(user, itemId) }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (item == null) {
            Text("Item not found.")
            Button(onClick = { vm.screen = Screen.ItemsList }) { Text("Back") }
            return@Column
        }
        Text(item.title, style = MaterialTheme.typography.headlineSmall)
        Text("Status: ${statusLabel(item)}", fontWeight = FontWeight.Bold)
        Text("Owner: ${vm.directory.title(item.ownerCallingId)}")
        Text("Due: ${item.dueDate}")
        Text("From: ${item.source.label}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("History", style = MaterialTheme.typography.titleMedium)
        for (entry in item.history) {
            Text("${entry.date} | ${entry.by}: ${entry.note}")
        }
        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text("Note or result") }, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (note.isNotBlank() && vm.tracker.addNote(user, item.id, note.trim())) {
                    note = ""
                    vm.refresh()
                }
            }) { Text("Add note") }
            if (item.status == Status.OPEN) {
                Button(onClick = {
                    val result = if (note.isBlank()) "Completed in the app" else note.trim()
                    if (vm.tracker.complete(user, item.id, result)) {
                        note = ""
                        vm.refresh()
                    }
                }) { Text("Mark done") }
            }
        }
        OutlinedButton(onClick = { vm.screen = Screen.ItemsList }) { Text("Back to list") }
    }
}
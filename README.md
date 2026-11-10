# Overview

**Ward Task Tracker** is a Kotlin console program that models a role-based task list for a ward leadership team. Each person who uses the app has a role (Bishop, Counselor, or Organization Leader), and each role sees a different set of tasks. Users can list tasks, add tasks, mark tasks as done, and see the completion percentage for each organization.

All names, organizations, and tasks in this project are **fictional sample data**. No real member information is used.

I wrote this program to learn the basics of Kotlin and to design the data model for a larger idea: later in this course I plan to turn the same concepts into an Android app (Module 2) and connect it to a cloud database (Module 3).

How the program meets the Kotlin module requirements:

| Requirement | Where it is in `src/Main.kt` |
|---|---|
| Variables (mutable and immutable) | `val` for fixed values such as `organizations`; `var` for `nextId`, `user`, and `running` |
| Expressions | Completion percentage (`finished * 100 / orgTasks.size`), `LocalDate.now().plusDays(...)` |
| Conditionals | `if/else`, `when` in `printTask`, and input validation in the menu |
| Loops | `for` loops that list tasks, and a `while` loop that keeps the menu running |
| Functions | `addTask`, `completeTask`, `tasksFor`, `progressFor`, `printTask`, `readNonEmpty`, and others |
| Classes | `TaskManager` class and the `Role` enum class |
| Extra requirement: data classes | `Task` and `Member` are `data class` types; `copy()` is used in `completeTask` to update a task |

I also used collections (`mutableListOf`, `filter`, `count`) and the `when` keyword with an enum for the role permissions.

{Link to the demo video: ADD YOUR VIDEO LINK HERE}

# Instructions for Build and Use

Steps to build and/or run the software:

1. Install a JDK (version 17 or later) and IntelliJ IDEA Community Edition.
2. Clone this repository: `git clone <your-repository-url>`
3. Open the folder in IntelliJ IDEA, or create a new Kotlin (JVM) project and place `src/Main.kt` in it.
4. Run the `main` function in `Main.kt` (the green arrow next to `fun main()`).

Command-line option (if the Kotlin compiler is installed):

```
kotlinc src/Main.kt -include-runtime -d wardtasktracker.jar
java -jar wardtasktracker.jar
```

Instructions for using the software:

1. Choose who is using the app (Bishop, Counselor, or Organization Leader).
2. Type the number of a menu option and press Enter.
3. Option 1 lists the tasks that your role is allowed to see. Option 2 adds a task to one of your organizations. Option 3 marks a task as done. Option 4 shows the completion percentage per organization. Option 5 switches to another user so you can compare what each role sees.
4. Type 0 to exit.

# Development Environment

To recreate the development environment, you need the following software and/or libraries with the specified versions:

* Kotlin 2.x (JVM)
* JDK 17 or later (tested with OpenJDK 21)
* IntelliJ IDEA Community Edition (free)
* Git and a GitHub account

The program uses only the Kotlin standard library and `java.time.LocalDate`. No external libraries are needed.

# Useful Websites to Learn More

I found these websites useful in developing this software:

* [Kotlin official documentation](https://kotlinlang.org/docs/home.html)
* [Kotlin data classes](https://kotlinlang.org/docs/data-classes.html)
* [Kotlin conditions and loops (including `when`)](https://kotlinlang.org/docs/control-flow.html)
* [Kotlin collections overview](https://kotlinlang.org/docs/collections-overview.html)

# Future Work

The following items I plan to fix, improve, and/or add to this project in the future:

* Turn the program into an Android app with a login screen and screens for each role (Module 2)
* Store the tasks in a cloud database and add user authentication (Module 3)
* Save tasks to a file so they remain after the program closes
* Add due-date reminders for overdue tasks
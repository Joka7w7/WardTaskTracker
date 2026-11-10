# Overview

As a software engineer, I want to build tools that solve real, everyday coordination problems, and I wanted to deepen my skills in a modern language. This project is my practice with Kotlin: I used it to model how work is assigned and followed up in a volunteer leadership team, where plans, reminders, and decisions are often scattered across paper notes, spreadsheets, and chat messages, making it hard to look back at what was decided and who was supposed to follow up.

**Ward Task Tracker** is a Kotlin console program that tracks action items from meetings. Each item is attached to a calling (a role such as Primary President or Ministering Brother). Roles form a chain of command from the bishop down to teachers and ministering brothers and sisters, and each person can see and assign only the items for their own role and the roles below it. Every item keeps a history log (who did what, and when), items can be marked confidential, and a reminders option lists items that are overdue or due soon. All names and tasks are fictional sample data.

My purpose for writing this software was to learn Kotlin syntax and idioms by building something useful, and to design the data model and rules that I plan to reuse later in an Android app with a cloud database.

The Kotlin features demonstrated in the code include:

- `val` and `var` for immutable and mutable variables
- Expressions and string templates (for example, `"Task #$id"`)
- Conditionals with `if/else` and the `when` expression
- `for` and `while` loops
- Functions, including functions with default parameters and expression bodies
- Classes (`Directory`, `Tracker`) and enum classes
- Data classes (`Calling`, `Person`, `LogEntry`, `ActionItem`) with `copy()`
- Collections (`List`, `Map`, `Set`, `mutableListOf`), `filter`, `associateBy`, and recursion
- Null safety with `?`, `?.`, and `?:`

[Software Demo Video](http://youtube.link.goes.here)

# Development Environment

I developed the software in IntelliJ IDEA Community Edition, using Git for version control and GitHub to host the repository.

The program is written in Kotlin 2.x and runs on the JVM with JDK 17 or later. It uses only the Kotlin standard library and `java.time.LocalDate`, so no external libraries are needed.

To run it:

1. Clone the repository and open the folder in IntelliJ IDEA (or create a Kotlin JVM project and place `src/Main.kt` in it).
2. Run the `main` function in `Main.kt`.

Or, from a terminal with the Kotlin compiler installed:

```
kotlinc src/Main.kt -include-runtime -d wardtasktracker.jar
java -jar wardtasktracker.jar
```

When the program starts, choose a user (for example, the Bishop, a counselor, or a teacher) and compare what each one can see. Type 0 to exit.

# Useful Websites

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Data Classes](https://kotlinlang.org/docs/data-classes.html)
- [Kotlin Conditions and Loops](https://kotlinlang.org/docs/control-flow.html)
- [Kotlin Collections Overview](https://kotlinlang.org/docs/collections-overview.html)

# Future Work

- Turn the program into an Android app with a login and a screen for each role
- Store the data in a cloud database and add user authentication
- Add recurring reminders (weekly, monthly, quarterly) for standard responsibilities
- Add calling-process tracking (recommended, approved, sustained, set apart)
- Extend the chain of command to the stake level
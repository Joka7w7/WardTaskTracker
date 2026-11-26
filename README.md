# Overview

As a software engineer, I want to build tools that solve real coordination problems, and I wanted to learn how to build a mobile app for Android. **Ward Task Tracker** is an Android app that helps volunteer leaders keep track of follow-ups ("action items") that come out of meetings. In many wards and similar volunteer teams, plans, reminders, and assignments are kept on paper, in spreadsheets, or in chat messages, so later it is hard to see what was discussed, who agreed to do what, and who has to follow up.

In this app, every action item is attached to a calling (a role such as Primary President or Ministering Brother). Roles form a chain of command from the bishop down to teachers and ministering brothers and sisters. A leader can see and assign items only for his or her own role and the roles below it, and every item keeps a history so nothing is lost. All names, roles, and tasks in this project are **fictional sample data**. No real member information is used.

**How to use the app:**

1. **Role Picker:** choose which sample person is using the app (for example, Bishop Smith, Sister Cruz, or Sister Diaz).
2. **Items List:** see the items that person is allowed to see. Red cards are overdue, grey cards are done, and the top of the screen shows the number of reminders and the chain of command. Try different people and compare what each one sees. For example, the Bishop sees everything, the Primary President sees only Primary items, and a Primary Teacher sees nothing.
3. **Add item:** tap "Add item", write what needs to be done, choose who it is assigned to (only your role or roles below), choose the meeting it came from, enter the number of days until it is due, and optionally mark it confidential. Tap "Save".
4. **Item Detail:** tap an item to see its full history. Write a note and tap "Add note", or tap "Mark done" to complete it. Every action is recorded in the history with the date and the person.

My purpose for creating this app was to learn Android development with Kotlin and Jetpack Compose, and to turn the logic of a Kotlin console program (also included in this repository) into a real interface that leaders could use on their phones. This is the first step toward an app that stores its data in the cloud and signs each leader in with their own account.

The app includes four screens and responds to the user's input. The original console version of the program is in the `kotlin-console` folder, and the Android app is in the `android-app` folder.

[Software Demo Video](https://youtu.be/SL-7Vzxia8Q)

# Development Environment

I developed the app in Android Studio, using Git for version control and GitHub to host the repository. I tested it on a physical Android phone connected by USB (with USB debugging turned on), and it can also run on the Android Studio emulator.

The app is written in **Kotlin**. The screens are built with **Jetpack Compose** and Material 3, and the app state is kept in an Android `ViewModel`. The business rules (the chain of command, who can see and assign items, and the history of each item) are plain Kotlin classes in `WardModel.kt`. The project is built with Gradle, and the minimum Android version is 8.0 (API 26). No external libraries were added beyond those that come with the Android Studio Empty Activity template.

To run it:

1. Install Android Studio and open the `android-app` folder.
2. Wait for Gradle to finish syncing.
3. Choose a device (an emulator, or a phone with USB debugging) and press the green Run button.

# Useful Websites

* [Android Studio](https://developer.android.com/studio)
* [Build Your First Android App](https://developer.android.com/training/basics/firstapp)
* [Android Basics in Kotlin](https://developer.android.com/courses/android-basics-kotlin/course)
* [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
* [Run Apps on a Hardware Device](https://developer.android.com/studio/run/device)
* [Kotlin Documentation](https://kotlinlang.org/docs/home.html)

# Future Work

* Save the items on the phone so they remain after the app closes
* Move the data to a cloud database so leaders share the same information
* Add user sign-in so each leader sees only what belongs to his or her role
* Add a date picker, recurring reminders (weekly, monthly, quarterly), and phone notifications
* Add calling-process tracking (recommended, approved, sustained, set apart)
* Extend the chain of command to the stake level
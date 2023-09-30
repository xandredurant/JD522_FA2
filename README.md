# JD522_FA2 - Java Task Manager Application

JD522_FA2 is a Java-based Task Manager application that allows users to manage tasks, organize them by categories, and perform various operations like adding, editing, deleting, and filtering tasks. It also provides features for saving and loading tasks from files, as well as exporting tasks to CSV.
## Project Structure

The project is organized as follows:
```
JD522_FA2/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── mycompany/
│                   └── jd522_fa2/
│                       ├── JD522_FA2.java
│                       ├── Task.java
│                       ├── TaskManagerApp.form
│                       └── TaskManagerApp.java
│
├── target/
│   ├── classes/
│   │   └── com/
│   │       └── mycompany/
│   │           └── jd522_fa2/
│   │               ├── (Compiled Java class files)
│   ├── JD522_FA2-1.0-SNAPSHOT.jar
│   ├── maven-archiver/
│   │   └── pom.properties
│   └── maven-status/
│       └── maven-compiler-plugin/
│           └── compile/
│               └── default-compile/
│                   ├── createdFiles.lst
│                   └── inputFiles.lst
│
├── TaskManager_CSV.csv
└── TaskManager_save_20230927012427.txt
```
## Prerequisites

Before running the JD522_FA2 application, make sure you have the following prerequisites installed:

1. Java Development Kit (JDK) - To compile and run the Java code.
2. Apache Maven - To build and package the project.
3. Database - Ensure you have a database server (e.g., MySQL) set up and running. You'll need to configure the database connection in the application.

## Getting Started

1. Clone the JD522_FA2 repository to your local machine:

   ```shell
   git clone https://github.com/xandredurant/JD522_FA2.git

2. Navigate to the project directory:
   cd JD522_FA2

3. Build the project using Maven:
   mvn clean package

4. Run the application:
   java -jar target/JD522_FA2-1.0-SNAPSHOT.jar
   
5. The JD522_FA2 application GUI will open, allowing you to manage tasks.

## Features
Add Task: Enter task details, select a completion status, and assign it to a category. Click the "Add" button to add the task to the list.

Edit Task: Select a task from the list, make changes to its details, and click the "Edit" button to save the changes.

Delete Task: Select a task from the list and click the "Delete" button to remove it. You can also delete all tasks with the same name.

Filter Tasks: Apply filters to display tasks based on completion status and category.

Save/Load Tasks: Save tasks to a text file or load tasks from a saved file.

Export Tasks: Export tasks to a CSV file.

## License
This project is licensed under the [MIT License](LICENSE.txt) - see the [LICENSE.txt](LICENSE.txt) file for details.

## Acknowledgments
This project was created as part of the JD522 course.

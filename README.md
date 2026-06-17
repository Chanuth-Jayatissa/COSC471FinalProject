# COSC471 Final Project

COSC471FinalProject is a Java database systems project that implements a small command-driven DBMS. The project includes command parsing, table/database management, file handling, and binary-search-tree indexing concepts for database operations.

## Features

- Command parser for DBMS-style operations.
- Database and table management classes.
- Binary search tree data structure for indexed access.
- File manager for persistence-oriented operations.
- Support direction for commands such as CREATE, USE, DESCRIBE, SELECT, INSERT, UPDATE, DELETE, INPUT, and EXIT.

## Tech Stack

- Java
- Custom command parser
- Binary search tree indexing
- File-based persistence concepts

## Project Structure

- DBMSApp.java - application entry point
- DBMS.java - DBMS command execution layer
- CommandParser.java - command parsing logic
- Database.java - database-level behavior
- Table.java - table-level behavior
- BinarySearchTree.java - indexing data structure
- FileManager.java - file operations

## Getting Started

Compile and run the Java app from the repository root:

```bash
javac *.java
java DBMSApp
```

If using VS Code, install the Java extension pack or equivalent Java tooling before running/debugging.

## Status

Coursework project for database systems. The README now summarizes the implementation instead of leading with assignment setup notes.

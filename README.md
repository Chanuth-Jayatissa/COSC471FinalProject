# DATABASE PROJECT

**DATABASE PROJECT – 100 points**

Implement a DBMS as defined below.

**Commands are:**  
(CREATE | USE | DESCRIBE | SELECT | LET | INSERT | UPDATE | DELETE | INPUT | EXIT)

Each command is fully defined below:

---

## 1. CREATE DATABASE and USE

a. **CREATE DATABASE Dbname;**  
&nbsp;&nbsp;&nbsp;&nbsp;Creates a new database with the given name.

b. **USE Dbname;**  
&nbsp;&nbsp;&nbsp;&nbsp;Sets the current database.

---

## 2. CREATE TABLE

**Syntax:**


- **AttrName:**  
  An identifier (alphabet followed by up to 19 alphanumeric characters).

- **Domain:**  
  A data type defined as one of the following:
  - **Integer:**  
    You may assume a 16 or 32-bit sized integer.
  - **Text:**  
    Text with a maximum of 100 characters.
  - **Float:**  
    Format: `Integer[.Digit[Digit]]` (i.e., an integer or a floating point number with one or two digits after the decimal).

**Details:**

- Creates a table with the given attributes and types.
- The first attribute may be specified as the primary key.
- **Build a binary search tree (BST) with the given index.**  
  - If a table has a primary key, then all retrievals from the table **must use the primary key index**.  
    For example, given:  
    ```
    create table student (id integer primary key, name text);
    ```
    - Searching for _"the name of the student with id 123"_ should be via the BST.
  - If a search is on a non-key attribute (e.g., _"the id or ids of students named John Doe"_), perform an **in-order traversal** of the BST. This ensures that everyone obtains the same sequence of names for the result.
- If there is no primary key, you may scan the tuples in any order.

---

## 3. SELECT Operation

**Syntax:**


- **AttrNameList:**  
  `AttrName [,AttrName]*`

- **TableNameList:**  
  `TableName [,TableName]*`

- **RelOp:**  
  `<, >, <=, >=, =, !=`

- **Constant:**  
  Either an integer constant, a string constant, or a float constant.
  - **IntConst:**  
    `-2^31 .. 2^31-1`
  - **StringConst:**  
    `" "` enclosing up to 30 characters.
  - **FloatConst:**  
    `IntConst [. IntConst]`

- **Condition:**  

**Output:**

Displays to the screen the rows (with column headers) that match the SELECT condition or “Nothing found” if there is no match.  
The rows should be numbered (e.g., 1., 2., etc.).

---

## 4. DESCRIBE

**Syntax:**


**Output:**

Displays the listed table or ALL tables and their attributes with data types.  
Also indicates the primary key attribute(s).

*Example:*


---

## 5. LET

**Syntax:**


**Output:**

Stores the result of the SELECT operation under the given TableName with `AttrName` as the key.  
*Note:* This involves creating a BST based on the key for TableName.  
If the key attribute is not among the selected attributes, give an error message and abort the query.

---

## 6. RENAME

**Syntax:**


**Output:**

Renames all attributes of TableName to the new names provided in AttrNameList.  
The number of new names must equal the number of attributes in TableName.

---

## 7. INSERT

**Syntax:**


**Output:**

Checks domain constraints, key constraints, and entity integrity constraints for the new tuple.  
If all checks pass, the new tuple is inserted into TableName.  
*Note:* Do not worry about referential integrity constraints.

---

## 8. UPDATE

**Syntax:**


**Output:**

Updates the tuples in TableName that satisfy the WHERE condition with the new SET values.

---

## 9. DELETE

**Syntax:**


**Output:**

Deletes tuples from TableName that satisfy the WHERE condition.  
If the WHERE clause is omitted, then all tuples are deleted and the relation schema for the table is removed from the database.

---

## 10. INPUT / OUTPUT

**Syntax:**


**Output:**

Reads and executes the commands from FileName1.  
If FileName2 is specified, the results are written to that file.

---

## 11. EXIT

**Syntax:**


**Output:**

Terminates program execution.  
Before exit, the schemas and data must be saved so that they are available the next time the program is executed.

---

## REQUIREMENTS

- All systems are to be demonstrated in person (in my office) or via Zoom.
- Demonstrations can be done during my office hours up to the last regular meeting day for the class.  
  **No demonstration will be scheduled on final exam day.**
- Groups of one to four students are allowed.  
  If students drop from a group, the rest of the group are responsible for all work.

**Indexing Requirements:**

- All searches based on the primary key attribute must be done by first searching the index, and if the key is matched, use the record pointer to access the corresponding record from the file.
- For tables with primary keys, when a search is on a non-key attribute, you must perform an **in-order traversal** of the BST index and retrieve the table records in the same order as the in-order traversal of the BST index.  
  This ensures that every student will have the same sequence of results for test data, even if records are stored differently based on deletion implementations.

---

## Implementation Suggestions

- Use recursive descent parsing for the grammar.  
  (If you are unfamiliar with recursive descent parsing, you may use any parsing technique you prefer.)
- Each table should have its own file.  
  The header of the file will contain the table description (number of attributes, attribute names, types, and sizes).  
  The tuples follow the header.
- Use a separate file to save the BST index for a table.

---

## Rubric

- **Numbers 1 to 10 are worth 10 points each.**  
  **Each part must work completely for you to get full credit.**

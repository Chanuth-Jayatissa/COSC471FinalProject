import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class CommandParser {

    /**
     * Parses an input command string and returns a DBMS.Command object.
     * The input is trimmed and its trailing semicolon (if present) is removed.
     */
    public static DBMS.Command parse(String input) throws Exception {
        input = input.trim();
        if (input.endsWith(";")) {
            input = input.substring(0, input.length() - 1);
        }
        String inputUpper = input.toUpperCase();

        if (inputUpper.startsWith("CREATE DATABASE")) {
            String[] tokens = input.split("\\s+");
            if (tokens.length < 3) {
                throw new IllegalArgumentException("CREATE DATABASE command requires a database name.");
            }
            String dbName = tokens[2];
            return new CreateDatabaseCommand(dbName);
        } else if (inputUpper.startsWith("CREATE TABLE")) {
            return new CreateTableCommand(input);
        } else if (inputUpper.startsWith("USE")) {
            String[] tokens = input.split("\\s+");
            if (tokens.length < 2) {
                throw new IllegalArgumentException("USE command requires a database name.");
            }
            String dbName = tokens[1];
            return new UseDatabaseCommand(dbName);
        } else if (inputUpper.startsWith("DESCRIBE")) {
            return new DescribeCommand(input);
        } else if (inputUpper.startsWith("SELECT")) {
            return new SelectCommand(input);
        } else if (inputUpper.startsWith("LET")) {
            return new LetCommand(input);
        } else if (inputUpper.startsWith("RENAME")) {
            return new RenameCommand(input);
        } else if (inputUpper.startsWith("INSERT")) {
            return new InsertCommand(input);
        } else if (inputUpper.startsWith("UPDATE")) {
            return new UpdateCommand(input);
        } else if (inputUpper.startsWith("DELETE")) {
            return new DeleteCommand(input);
        } else if (inputUpper.startsWith("INPUT")) {
            return new InputCommand(input);
        } else if (inputUpper.startsWith("SHOW")) {
            return new ShowCommand(input);
        } else if (inputUpper.equals("EXIT")) {
            return new ExitCommand();
        } else {
            throw new UnsupportedOperationException("Command not supported: " + input);
        }
    }

    // -------------------- Command Inner Classes --------------------

    public static class CreateDatabaseCommand implements DBMS.Command {
        private String dbName;

        public CreateDatabaseCommand(String dbName) {
            this.dbName = dbName;
        }

        @Override
        public void execute(DBMS dbms) {
            dbms.createDatabase(dbName);
        }
    }

    public static class CreateTableCommand implements DBMS.Command {
        private String tableName;
        private java.util.List<Table.Attribute> attributes = new java.util.ArrayList<>();

        /**
         * Expected format:
         * CREATE TABLE tableName ( attrName dataType [PRIMARY KEY], attrName dataType,
         * ... )
         */
        public CreateTableCommand(String input) throws Exception {
            String remainder = input.substring("CREATE TABLE".length()).trim();
            int parenStart = remainder.indexOf('(');
            if (parenStart == -1) {
                throw new IllegalArgumentException("Missing '(' for attribute list.");
            }
            tableName = remainder.substring(0, parenStart).trim();
            int parenEnd = remainder.lastIndexOf(')');
            if (parenEnd == -1) {
                throw new IllegalArgumentException("Missing ')' for attribute list.");
            }
            String attrListStr = remainder.substring(parenStart + 1, parenEnd).trim();
            String[] attrTokens = attrListStr.split(",");
            for (String token : attrTokens) {
                token = token.trim();
                // Expect format: attrName dataType [PRIMARY KEY]
                String[] parts = token.split("\\s+");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid attribute definition: " + token);
                }
                String attrName = parts[0];
                String dataTypeStr = parts[1].toUpperCase();
                Table.Attribute.DataType dataType;
                if (dataTypeStr.equals("INTEGER")) {
                    dataType = Table.Attribute.DataType.INTEGER;
                } else if (dataTypeStr.equals("FLOAT")) {
                    dataType = Table.Attribute.DataType.FLOAT;
                } else if (dataTypeStr.equals("TEXT")) {
                    dataType = Table.Attribute.DataType.TEXT;
                } else {
                    throw new IllegalArgumentException("Unknown data type: " + dataTypeStr);
                }
                boolean primaryKey = false;
                if (parts.length >= 3 && parts[2].toUpperCase().equals("PRIMARY")) {
                    primaryKey = true;
                }
                attributes.add(new Table.Attribute(attrName, dataType, primaryKey));
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected. Use the USE command first.");
                return;
            }
            Table newTable = new Table(tableName, attributes);
            dbms.getCurrentDatabase().addTable(tableName, newTable);
        }
    }

    public static class UseDatabaseCommand implements DBMS.Command {
        private String dbName;

        public UseDatabaseCommand(String dbName) {
            this.dbName = dbName;
        }

        @Override
        public void execute(DBMS dbms) {
            dbms.useDatabase(dbName);
        }
    }

    public static class DescribeCommand implements DBMS.Command {
        private boolean describeAll;
        private String tableName;

        /**
         * Supports:
         * DESCRIBE ALL
         * DESCRIBE tableName
         */
        public DescribeCommand(String input) {
            String remainder = input.substring("DESCRIBE".length()).trim();
            if (remainder.equalsIgnoreCase("ALL")) {
                describeAll = true;
            } else {
                describeAll = false;
                tableName = remainder;
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            if (describeAll) {
                for (String tName : dbms.getCurrentDatabase().listTables()) {
                    printTableSchema(dbms.getCurrentDatabase().getTable(tName));
                }
            } else {
                Table t = dbms.getCurrentDatabase().getTable(tableName);
                if (t != null)
                    printTableSchema(t);
            }
        }

        private void printTableSchema(Table table) {
            System.out.println("Table: " + table.getName());
            for (Table.Attribute attr : table.getAttributes()) {
                System.out.print(" - " + attr.getName() + " : " + attr.getDataType());
                if (attr.isPrimaryKey()) {
                    System.out.print(" (PRIMARY KEY)");
                }
                System.out.println();
            }
        }
    }

    public static class SelectCommand implements DBMS.Command {
        private java.util.List<String> columns = new java.util.ArrayList<>();
        private java.util.List<String> tableNames = new java.util.ArrayList<>();
        private String condition = "";

        /**
         * Expected format (simplified):
         * SELECT col1, col2, ... FROM tableName1 [, tableName2, ...] [WHERE condition]
         */
        public SelectCommand(String input) throws Exception {
            String remainder = input.substring("SELECT".length()).trim();

            int fromIndex = remainder.toUpperCase().indexOf("FROM");
            if (fromIndex == -1) {
                throw new IllegalArgumentException("SELECT command must contain FROM clause.");
            }

            // Parse SELECT columns.
            String colsPart = remainder.substring(0, fromIndex).trim();
            String[] cols = colsPart.split(",");
            for (String col : cols) {
                columns.add(col.trim());
            }

            // Parse the FROM part.
            String afterFrom = remainder.substring(fromIndex + "FROM".length()).trim();
            int whereIndex = afterFrom.toUpperCase().indexOf("WHERE");
            String tablesPart;
            if (whereIndex != -1) {
                tablesPart = afterFrom.substring(0, whereIndex).trim();
                condition = afterFrom.substring(whereIndex + "WHERE".length()).trim();
            } else {
                tablesPart = afterFrom;
            }
            String[] tables = tablesPart.split(",");
            for (String table : tables) {
                tableNames.add(table.trim());
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            if (tableNames.size() > 1) {
                // Multi-table join (Cartesian product)
                List<List<Table.Record>> listOfRecordLists = new ArrayList<>();
                // Build a combined schema with qualified attribute names.
                List<Table.Attribute> combinedSchema = new ArrayList<>();

                for (String tName : tableNames) {
                    Table t = dbms.getCurrentDatabase().getTable(tName);
                    if (t == null) {
                        System.out.println("Error: Table '" + tName + "' does not exist.");
                        return;
                    }
                    listOfRecordLists.add(t.getRecords());
                    for (Table.Attribute attr : t.getAttributes()) {
                        // Qualify attribute name with table name.
                        combinedSchema.add(new Table.Attribute(tName + "." + attr.getName(), attr.getDataType(),
                                attr.isPrimaryKey()));
                    }
                }

                // Compute the Cartesian product (join).
                List<Table.Record> joinedRecords = cartesianProduct(listOfRecordLists);

                // Filter the joined records if a condition is provided.
                List<Table.Record> finalRecords = new ArrayList<>();
                if (condition != null && !condition.trim().isEmpty()) {
                    for (Table.Record rec : joinedRecords) {
                        // Evaluate condition on the joined record using the combined schema.
                        if (evaluateConditionOnJoinedRecord(rec, combinedSchema, condition)) {
                            finalRecords.add(rec);
                        }
                    }
                } else {
                    finalRecords = joinedRecords;
                }

                // For simplicity, assume that the SELECT list columns refer to the names in the
                // combined schema.
                System.out.println(String.join("\t", columns));
                int count = 1;
                for (Table.Record rec : finalRecords) {
                    System.out.print(count + ".\t");
                    List<Object> vals = rec.getValues();
                    // For each column in the SELECT list, find its index in the combined schema.
                    for (String col : columns) {
                        int idx = findIndexInCombinedSchema(combinedSchema, col);
                        if (idx != -1 && idx < vals.size()) {
                            System.out.print(vals.get(idx) + "\t");
                        } else {
                            System.out.print("NULL\t");
                        }
                    }
                    System.out.println();
                    count++;
                }
            } else {
                // Single table select (existing behavior)
                String tableName = tableNames.get(0);
                Table table = dbms.getCurrentDatabase().getTable(tableName);
                if (table == null)
                    return;
                java.util.List<Table.Record> records = table.select(condition);
                if (records.isEmpty()) {
                    System.out.println("Nothing found.");
                    return;
                }
                System.out.println(String.join("\t", columns));
                int count = 1;
                for (Table.Record record : records) {
                    System.out.print(count + ".\t");
                    java.util.List<Object> vals = record.getValues();
                    for (String col : columns) {
                        int idx = -1;
                        java.util.List<Table.Attribute> attrs = table.getAttributes();
                        for (int i = 0; i < attrs.size(); i++) {
                            if (attrs.get(i).getName().equalsIgnoreCase(col)) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx != -1 && idx < vals.size()) {
                            System.out.print(vals.get(idx) + "\t");
                        } else {
                            System.out.print("NULL\t");
                        }
                    }
                    System.out.println();
                    count++;
                }
            }
        }

        /**
         * Computes the Cartesian product (cross join) of a list of record lists.
         */
        private List<Table.Record> cartesianProduct(List<List<Table.Record>> listOfRecordLists) {
            List<Table.Record> result = new ArrayList<>();
            if (listOfRecordLists.isEmpty()) {
                result.add(new Table.Record(new ArrayList<>()));
                return result;
            }
            cartesianProductHelper(listOfRecordLists, 0, new ArrayList<>(), result);
            return result;
        }

        private void cartesianProductHelper(List<List<Table.Record>> listOfRecordLists, int index, List<Object> current,
                List<Table.Record> result) {
            if (index == listOfRecordLists.size()) {
                result.add(new Table.Record(new ArrayList<>(current)));
                return;
            }
            for (Table.Record rec : listOfRecordLists.get(index)) {
                int initialSize = current.size();
                current.addAll(rec.getValues());
                cartesianProductHelper(listOfRecordLists, index + 1, current, result);
                while (current.size() > initialSize) {
                    current.remove(current.size() - 1);
                }
            }
        }

        /**
         * Evaluates a condition on a joined record given the combined schema.
         * For simplicity, this uses the same condition parser from Table.
         */
        private boolean evaluateConditionOnJoinedRecord(Table.Record record, List<Table.Attribute> combinedSchema,
                String conditionStr) {
            try {
                // Use the existing parseCondition method from the Table instance.
                // For this evaluation, we pass the combinedSchema in place of Table's own
                // schema.
                Table.Condition condition = Table.parseCondition(conditionStr, combinedSchema);
                return condition.evaluate(record, combinedSchema);
            } catch (Exception e) {
                System.out.println("Error evaluating condition on joined record: " + e.getMessage());
                return false;
            }
        }

        /**
         * Finds the index of the column in the combined schema.
         */
        private int findIndexInCombinedSchema(List<Table.Attribute> combinedSchema, String colName) {
            // 1) exact (qualified) match
            for (int i = 0; i < combinedSchema.size(); i++) {
                if (combinedSchema.get(i).getName().equalsIgnoreCase(colName)) {
                    return i;
                }
            }
            
            // 2) fallback: unqualified suffix match
            for (int i = 0; i < combinedSchema.size(); i++) {
                String full = combinedSchema.get(i).getName();
                int dot = full.indexOf('.');
                if (dot >= 0 && full.substring(dot + 1).equalsIgnoreCase(colName)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class LetCommand implements DBMS.Command {
        private String newTableName;
        private String keyAttribute;
        private SelectCommand selectCommand;

        /**
         * Expected format:
         * LET newTableName KEY keyAttribute SELECT ...
         */
        public LetCommand(String input) throws Exception {
            input = input.trim(); // Remove excess white space on ends
            int keyIndex = input.toUpperCase().indexOf("KEY");
            if (keyIndex == -1) {
                throw new IllegalArgumentException("LET command must contain KEY.");
            }
            // Remove the LET keyword and white space = "newTableName KEY keyAttr SELECT
            // ..."
            input = input.substring(3).trim();
            keyIndex = input.toUpperCase().indexOf("KEY"); // Update key index

            newTableName = input.substring(0, keyIndex).trim();
            if (newTableName.split("\s").length > 1) {
                throw new IllegalArgumentException("Table name must be one word. Your name was: " + newTableName);
            }
            // After KEY = "keyAttribute SELECT ..."
            input = input.substring(keyIndex + 3).trim();
            keyIndex = input.toUpperCase().indexOf("KEY");

            // Grab key attr and remaining SELECT string
            int selectIndex = input.toUpperCase().indexOf("SELECT");
            if (selectIndex == -1) {
                throw new IllegalArgumentException("LET command must contain a SELECT operation.");
            }

            keyAttribute = input.substring(0, selectIndex).trim();

            if (keyAttribute.split("\s").length > 1) {
                throw new IllegalArgumentException("KEY name must be one word");
            }
            // After Key attr = "SELECT ..."
            input = input.substring(selectIndex).trim();

            // Now to send the select command to the parser
            selectCommand = new SelectCommand(input);
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            System.out.println("Executing LET command: storing result into table '"
                    + newTableName + "' with key '" + keyAttribute + "'.");

            // Reference for the tables the select command referenced
            List<String> tableNames = selectCommand.tableNames;
            List<Table> sourceTables = new ArrayList<>();

            // Loop through source table list, adding tables to the internal list and
            // ensuring names are correct
            for (String tableName : tableNames) {
                Table table = dbms.getCurrentDatabase().getTable(tableName);
                if (table == null) {
                    System.out.println("Error: Source table '" + tableName + "' does not exist.");
                    return;
                }
                sourceTables.add(table);
            }

            // The new table, created by let command
            List<Table.Attribute> combinedSchema = new ArrayList<>();

            // For all tables in tableNames
            for (int i = 0; i < tableNames.size(); i++) {
                String tableName = tableNames.get(i);
                Table table = sourceTables.get(i);

                // For each attribute in the table
                for (Table.Attribute attr : table.getAttributes()) {

                    // Add attribute to new table
                    combinedSchema
                            .add(new Table.Attribute(tableName + "." + attr.getName(), attr.getDataType(), false));
                }
            }

            List<List<Table.Record>> listOfRecordLists = new ArrayList<>();

            // For each table in the source, add to the list of record lists
            for (Table table : sourceTables) {
                listOfRecordLists.add(table.getRecords());
            }

            // Joining the records
            List<Table.Record> joinedRecords = selectCommand.cartesianProduct(listOfRecordLists);

            List<Table.Record> selectedRecords = new ArrayList<>();

            // Apply WHERE condition if it exists
            if (!selectCommand.condition.isEmpty()) {
                // For each record in joined records
                for (Table.Record rec : joinedRecords) {
                    // If it follows the condition, add it to selected records
                    if (selectCommand.evaluateConditionOnJoinedRecord(rec, combinedSchema, selectCommand.condition)) {
                        selectedRecords.add(rec);
                    }
                }
            } else {
                // All records are selected
                selectedRecords = joinedRecords;
            }

            // Build the new schema using the selected columns
            java.util.List<Table.Attribute> newAttrs = new java.util.ArrayList<>();
            boolean keyFound = false;

            // For each column and attribute,
            for (String col : selectCommand.columns) {
                for (Table.Attribute attr : combinedSchema) {

                    // If attribute name = column name,
                    if (attr.getName().equalsIgnoreCase(col)) {
                        // then see if this attribute is the key attribute referenced in LET command
                        boolean isKey = attr.getName().equalsIgnoreCase(keyAttribute);
                        if (isKey)
                            keyFound = true;

                        // add the attribute, removing the prefixes, ie. student.name -> name
                        String cleanName = attr.getName().contains(".")
                                ? attr.getName().substring(attr.getName().indexOf(".") + 1)
                                : attr.getName();
                        newAttrs.add(new Table.Attribute(cleanName, attr.getDataType(), isKey));
                        break;
                    }
                }
            }
            // If the key is not in the select result
            if (!keyFound) {
                System.out.println("Error: Key attribute '" + keyAttribute + "' not found in SELECT result.");
                return;
            }

            // Create the new table
            Table newTable = new Table(newTableName, newAttrs);

            // Loop through each record matching the select query
            for (Table.Record oldRecord : selectedRecords) {
                java.util.List<Object> targetValues = new java.util.ArrayList<>(); // Values we want to keep
                java.util.List<Object> oldValues = oldRecord.getValues(); // full list of values from original record

                // For each column in select statement
                for (String col : selectCommand.columns) {
                    int idx = selectCommand.findIndexInCombinedSchema(combinedSchema, col);
                    if (idx != -1 && idx < oldValues.size()) {
                        // Add it to target
                        targetValues.add(oldValues.get(idx));
                    } else {
                        targetValues.add(null);
                    }
                }

                // Insert the values
                newTable.insert(new Table.Record(targetValues));
            }
            // Add the table to the database
            dbms.getCurrentDatabase().addTable(newTableName, newTable);
            System.out.println("LET: Table '" + newTableName + "' created with " +
                    newTable.getRecords().size() + " record(s).");
        }
    }

    public static class RenameCommand implements DBMS.Command {
        private String tableName;
        private java.util.List<String> newNames = new java.util.ArrayList<>();

        /**
         * Expected format:
         * RENAME tableName (newAttr1, newAttr2, ...)
         */
        public RenameCommand(String input) throws Exception {
            String remainder = input.substring("RENAME".length()).trim();
            int parenStart = remainder.indexOf('(');
            int parenEnd = remainder.indexOf(')');
            if (parenStart == -1 || parenEnd == -1) {
                throw new IllegalArgumentException("RENAME command must include new attribute names in parentheses.");
            }
            tableName = remainder.substring(0, parenStart).trim();
            String namesStr = remainder.substring(parenStart + 1, parenEnd).trim();
            String[] namesArr = namesStr.split(",");
            for (String name : namesArr) {
                newNames.add(name.trim());
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            Table table = dbms.getCurrentDatabase().getTable(tableName);
            if (table == null)
                return;
            table.renameAttributes(newNames);
        }
    }

    public static class InsertCommand implements DBMS.Command {
        private String tableName;
        private java.util.List<String> values = new java.util.ArrayList<>();

        /**
         * Expected format:
         * INSERT tableName VALUES (val1, val2, ..., valN)
         */
        public InsertCommand(String input) throws Exception {
            // grab everything after "INSERT"
            String remainder = input.substring("INSERT".length()).trim();
        
            // ——— NEW: reject any “INTO” usage ———
            if (remainder.toUpperCase().startsWith("INTO ")) {
                throw new IllegalArgumentException(
                    "INSERT command must be: INSERT <table> VALUES (...);  (no INTO allowed)");
            }
        
            // ——— then find VALUES as before ———
            int valuesIndex = remainder.toUpperCase().indexOf("VALUES");
            if (valuesIndex == -1) {
                throw new IllegalArgumentException("INSERT command must contain VALUES.");
            }
            tableName = remainder.substring(0, valuesIndex).trim();
            String valuesPart = remainder.substring(valuesIndex + "VALUES".length()).trim();
            if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
                throw new IllegalArgumentException("VALUES must be enclosed in parentheses.");
            }
            valuesPart = valuesPart.substring(1, valuesPart.length() - 1).trim();
            String[] vals = valuesPart.split(",");
            for (String val : vals) {
                values.add(val.trim().replaceAll("^\"|\"$", ""));
            }
        }
        

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            Table table = dbms.getCurrentDatabase().getTable(tableName);
            if (table == null)
                return;
            Table.Record record = new Table.Record(new java.util.ArrayList<Object>(values));
            table.insert(record);
        }
    }

    public static class UpdateCommand implements DBMS.Command {
        private String tableName;
        private java.util.Map<String, String> updates = new java.util.HashMap<>();
        private String condition = "";

        /**
         * Expected format:
         * UPDATE tableName SET attr=value [, attr=value]* [WHERE condition]
         */
        public UpdateCommand(String input) throws Exception {
            String remainder = input.substring("UPDATE".length()).trim();
            int setIndex = remainder.toUpperCase().indexOf("SET");
            if (setIndex == -1) {
                throw new IllegalArgumentException("UPDATE command must contain SET.");
            }
            tableName = remainder.substring(0, setIndex).trim();
            String updatesPart = remainder.substring(setIndex + "SET".length()).trim();
            int whereIndex = updatesPart.toUpperCase().indexOf("WHERE");
            if (whereIndex != -1) {
                condition = updatesPart.substring(whereIndex + "WHERE".length()).trim();
                updatesPart = updatesPart.substring(0, whereIndex).trim();
            }
            String[] assignments = updatesPart.split(",");
            for (String assign : assignments) {
                String[] parts = assign.split("=");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid assignment: " + assign);
                }
                String attr = parts[0].trim();
                String val = parts[1].trim().replaceAll("^\"|\"$", "");
                updates.put(attr, val);
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            Table table = dbms.getCurrentDatabase().getTable(tableName);
            if (table == null)
                return;
            int updatedCount = 0;
            for (Table.Record record : table.getRecords()) {
                if (condition.isEmpty() || table.matchesCondition(record, condition)) {
                    java.util.List<Table.Attribute> attrs = table.getAttributes();
                    java.util.List<Object> vals = record.getValues();
                    for (String attrName : updates.keySet()) {
                        for (int i = 0; i < attrs.size(); i++) {
                            if (attrs.get(i).getName().equalsIgnoreCase(attrName)) {
                                vals.set(i, updates.get(attrName));
                                break;
                            }
                        }
                    }
                    updatedCount++;
                }
            }
            System.out.println(updatedCount + " record(s) updated in table '" + tableName + "'.");
        }
    }

    public static class DeleteCommand implements DBMS.Command {
        private String tableName;
        private String condition = "";

        /**
         * Expected format:
         * DELETE tableName [WHERE condition]
         */
        public DeleteCommand(String input) throws Exception {
            String remainder = input.substring("DELETE".length()).trim();
            int whereIndex = remainder.toUpperCase().indexOf("WHERE");
            if (whereIndex == -1) {
                tableName = remainder;
                condition = "";
            } else {
                tableName = remainder.substring(0, whereIndex).trim();
                condition = remainder.substring(whereIndex + "WHERE".length()).trim();
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            Table table = dbms.getCurrentDatabase().getTable(tableName);
            if (table == null)
                return;
            int deletedCount = 0;
            java.util.Iterator<Table.Record> iter = table.getRecords().iterator();

            // No WHERE clause -> delete entire table and contents
            if (condition.isEmpty()) {
                dbms.getCurrentDatabase().deleteTable(tableName);
                System.out.println("Table '" + tableName + "' and all its records were deleted.");
                return;
            }
            // Remove tuples according to WHERE clause
            while (iter.hasNext()) {
                Table.Record record = iter.next();

                // Removes record if it meets the condition
                if (table.matchesCondition(record, condition)) {
                    iter.remove();
                    deletedCount++;
                }
            }
            System.out.println(deletedCount + " record(s) deleted from table '" + tableName + "'.");
        }

    }

    public static class InputCommand implements DBMS.Command {
        private String inputFile;
        private String outputFile; // May be null.

        /**
         * Expected format:
         * INPUT fileName1 [OUTPUT fileName2]
         */
        public InputCommand(String input) throws Exception {
            String remainder = input.substring("INPUT".length()).trim();
            String[] tokens = remainder.split("\\s+");
            if (tokens.length < 1) {
                throw new IllegalArgumentException("INPUT command requires at least an input file.");
            }
            inputFile = tokens[0];
            if (tokens.length >= 2 && tokens[1].equalsIgnoreCase("OUTPUT")) {
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("OUTPUT file name missing.");
                }
                outputFile = tokens[2];
            }
        }

        @Override
        public void execute(DBMS dbms) throws Exception {
            java.util.List<String> commands = FileManager.readCommands(inputFile);
            StringBuilder outBuilder = new StringBuilder();
            for (String cmdStr : commands) {
                try {
                    DBMS.Command cmd = CommandParser.parse(cmdStr);
                    cmd.execute(dbms);
                    outBuilder.append("Executed: ").append(cmdStr).append("\n");
                } catch (Exception e) {
                    outBuilder.append("Error executing command: ").append(cmdStr)
                            .append(" - ").append(e.getMessage()).append("\n");
                }
            }
            if (outputFile != null) {
                FileManager.writeOutput(outputFile, outBuilder.toString());
                System.out.println("Output written to " + outputFile);
            }
        }
    }

    public static class ExitCommand implements DBMS.Command {
        @Override
        public void execute(DBMS dbms) {
            dbms.saveState();
            System.out.println("Exiting DBMS. State has been saved.");
            System.exit(0);
        }
    }

    // -------------------- New Show Command --------------------
    /**
     * The SHOW command supports:
     * SHOW DATABASES;
     * SHOW TABLES;
     * SHOW RECORDS tableName;
     */
    public static class ShowCommand implements DBMS.Command {
        private String subCommand;
        private String tableName; // Used only if subCommand is RECORDS.

        public ShowCommand(String input) throws Exception {
            // Remove the "SHOW" keyword.
            String remainder = input.substring("SHOW".length()).trim();
            if (remainder.isEmpty()) {
                throw new IllegalArgumentException(
                        "SHOW command requires parameters (e.g., DATABASES, TABLES, RECORDS <tableName>).");
            }
            String[] parts = remainder.split("\\s+");
            subCommand = parts[0].toUpperCase();
            if (subCommand.equals("RECORDS")) {
                if (parts.length < 2) {
                    throw new IllegalArgumentException("SHOW RECORDS command requires a table name.");
                }
                tableName = parts[1];
            }
        }

        @Override
        public void execute(DBMS dbms) {
            if (subCommand.equals("DATABASES")) {
                java.util.Set<String> dbs = dbms.listDatabases();
                if (dbs.isEmpty()) {
                    System.out.println("No databases available.");
                } else {
                    System.out.println("Databases:");
                    for (String dbName : dbs) {
                        System.out.println(" - " + dbName);
                    }
                }
            } else if (subCommand.equals("TABLES")) {
                if (dbms.getCurrentDatabase() == null) {
                    System.out.println("Error: No database selected.");
                    return;
                }
                java.util.Set<String> tables = dbms.getCurrentDatabase().listTables();
                if (tables.isEmpty()) {
                    System.out.println("No tables in the current database.");
                } else {
                    System.out.println("Tables in database '" + dbms.getCurrentDatabase().getName() + "':");
                    for (String t : tables) {
                        System.out.println(" - " + t);
                    }
                }
            } else if (subCommand.equals("RECORDS")) {
                if (dbms.getCurrentDatabase() == null) {
                    System.out.println("Error: No database selected.");
                    return;
                }
                Table table = dbms.getCurrentDatabase().getTable(tableName);
                if (table == null) {
                    System.out.println("Error: Table '" + tableName + "' does not exist.");
                    return;
                }
                java.util.List<Table.Record> recs = table.getRecords();
                if (recs.isEmpty()) {
                    System.out.println("Table '" + tableName + "' is empty.");
                } else {
                    System.out.println(
                            "Records of table '" + tableName + "':" + "\n  ------------------------------------");
                    // For each attribute the table has
                    for (Table.Attribute attr : table.getAttributes()) {
                        // Print out the header
                        System.out.print("\t" + attr.getName());
                    }
                    System.out.println("\n  ------------------------------------");
                    int count = 1;
                    for (Table.Record r : recs) {
                        System.out.print(count + ".\t");
                        for (Object val : r.getValues()) {
                            System.out.print(val + "\t");
                        }
                        System.out.println();
                        count++;
                    }
                    System.out.println("  ------------------------------------");
                }
            } else {
                System.out.println("Invalid SHOW command parameter: " + subCommand);
            }
        }
    }
}

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
         *   CREATE TABLE tableName ( attrName dataType [PRIMARY KEY], attrName dataType, ... )
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
         *   DESCRIBE ALL
         *   DESCRIBE tableName
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
        private String tableName;
        private String condition = "";
        
        /**
         * Expected format (simplified):
         *   SELECT col1, col2 FROM tableName [WHERE condition]
         */
        public SelectCommand(String input) throws Exception {
            String remainder = input.substring("SELECT".length()).trim();
            int fromIndex = remainder.toUpperCase().indexOf("FROM");
            if (fromIndex == -1) {
                throw new IllegalArgumentException("SELECT command must contain FROM clause.");
            }
            String colsPart = remainder.substring(0, fromIndex).trim();
            String[] cols = colsPart.split(",");
            for (String col : cols) {
                columns.add(col.trim());
            }
            String afterFrom = remainder.substring(fromIndex + "FROM".length()).trim();
            int whereIndex = afterFrom.toUpperCase().indexOf("WHERE");
            if (whereIndex != -1) {
                tableName = afterFrom.substring(0, whereIndex).trim();
                condition = afterFrom.substring(whereIndex + "WHERE".length()).trim();
            } else {
                tableName = afterFrom;
            }
        }
        
        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            Table table = dbms.getCurrentDatabase().getTable(tableName);
            if (table == null) return;
            java.util.List<Table.Record> records = table.select(condition);
            if (records.isEmpty()) {
                System.out.println("Nothing found.");
                return;
            }
            // Print column headers.
            System.out.println(String.join("\t", columns));
            int count = 1;
            for (Table.Record record : records) {
                System.out.print(count + ".\t");
                java.util.List<Object> vals = record.getValues();
                // For each requested column, find its index and print its value.
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
    
    public static class LetCommand implements DBMS.Command {
        private String newTableName;
        private String keyAttribute;
        private SelectCommand selectCommand;
        
        /**
         * Expected format:
         *   LET newTableName KEY keyAttribute <SELECT ...>
         */
        public LetCommand(String input) throws Exception {
            int keyIndex = input.toUpperCase().indexOf("KEY");
            if (keyIndex == -1) {
                throw new IllegalArgumentException("LET command must contain KEY.");
            }
            String beforeKey = input.substring("LET".length(), keyIndex).trim();
            newTableName = beforeKey;
            int ltIndex = input.indexOf("<");
            int gtIndex = input.lastIndexOf(">");
            if (ltIndex == -1 || gtIndex == -1 || gtIndex <= ltIndex) {
                throw new IllegalArgumentException("LET command must contain a SELECT statement enclosed in <>.");
            }
            String keyPart = input.substring(keyIndex + "KEY".length(), ltIndex).trim();
            keyAttribute = keyPart;
            String selectStr = input.substring(ltIndex + 1, gtIndex).trim();
            if (!selectStr.toUpperCase().startsWith("SELECT")) {
                throw new IllegalArgumentException("LET command must contain a SELECT operation within <>.");
            }
            selectCommand = new SelectCommand(selectStr);
        }
        
        @Override
        public void execute(DBMS dbms) {
            if (dbms.getCurrentDatabase() == null) {
                System.out.println("Error: No database selected.");
                return;
            }
            System.out.println("Executing LET command: storing result into table '" 
                + newTableName + "' with key '" + keyAttribute + "'.");
            // In a full implementation, the SELECT operation would produce a result set.
            // Here, we simulate by creating an empty table with a dummy schema containing the key attribute.
            java.util.List<Table.Attribute> dummySchema = new java.util.ArrayList<>();
            dummySchema.add(new Table.Attribute(keyAttribute, Table.Attribute.DataType.TEXT, true));
            Table newTable = new Table(newTableName, dummySchema);
            dbms.getCurrentDatabase().addTable(newTableName, newTable);
        }
    }
    
    public static class RenameCommand implements DBMS.Command {
        private String tableName;
        private java.util.List<String> newNames = new java.util.ArrayList<>();
        
        /**
         * Expected format:
         *   RENAME tableName (newAttr1, newAttr2, ...)
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
            if (table == null) return;
            table.renameAttributes(newNames);
        }
    }
    
    public static class InsertCommand implements DBMS.Command {
        private String tableName;
        private java.util.List<String> values = new java.util.ArrayList<>();
        
        /**
         * Expected format:
         *   INSERT tableName VALUES (val1, val2, ..., valN)
         */
        public InsertCommand(String input) throws Exception {
            String remainder = input.substring("INSERT".length()).trim();
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
            if (table == null) return;
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
         *   UPDATE tableName SET attr=value [, attr=value]* [WHERE condition]
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
            if (table == null) return;
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
         *   DELETE tableName [WHERE condition]
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
            while (iter.hasNext()) {
                Table.Record record = iter.next();
                if (condition.isEmpty() || table.matchesCondition(record, condition)) {
                    iter.remove();
                    deletedCount++;
                }
            }
            System.out.println(deletedCount + " record(s) deleted from table '" + tableName + "'.");
        }
    }
    
    public static class InputCommand implements DBMS.Command {
        private String inputFile;
        private String outputFile;  // May be null.
        
        /**
         * Expected format:
         *   INPUT fileName1 [OUTPUT fileName2]
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
     *   SHOW DATABASES;
     *   SHOW TABLES;
     *   SHOW RECORDS tableName;
     */
    public static class ShowCommand implements DBMS.Command {
        private String subCommand;
        private String tableName;  // Used only if subCommand is RECORDS.
        
        public ShowCommand(String input) throws Exception {
            // Remove the "SHOW" keyword.
            String remainder = input.substring("SHOW".length()).trim();
            if (remainder.isEmpty()) {
                throw new IllegalArgumentException("SHOW command requires parameters (e.g., DATABASES, TABLES, RECORDS <tableName>).");
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
                    System.out.println("Records of table '" + tableName + "':");
                    int count = 1;
                    for (Table.Record r : recs) {
                        System.out.print(count + ".\t");
                        for (Object val : r.getValues()) {
                            System.out.print(val + "\t");
                        }
                        System.out.println();
                        count++;
                    }
                }
            } else {
                System.out.println("Invalid SHOW command parameter: " + subCommand);
            }
        }
    }
}

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private List<Attribute> attributes;
    private List<Record> records;
    private String primaryKey; // Name of the primary key attribute.
    
    // Uses a type-safe BST (implemented in BinarySearchTree) with a helper KeyWrapper.
    private BinarySearchTree<KeyWrapper, Record> bstIndex;
    
    public Table(String name, List<Attribute> attributes) {
        this.name = name;
        this.attributes = attributes;
        this.records = new ArrayList<>();
        // Look for a primary key in the schema.
        for (Attribute attr : attributes) {
            if (attr.isPrimaryKey()) {
                this.primaryKey = attr.getName();
                // Initialize the BST index.
                this.bstIndex = new BinarySearchTree<>();
                break;
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public List<Attribute> getAttributes() {
        return attributes;
    }
    
    public List<Record> getRecords() {
        return records;
    }
    
    public String getPrimaryKey() {
        return primaryKey;
    }
    
    public BinarySearchTree<KeyWrapper, Record> getBstIndex() {
        return bstIndex;
    }
    
    /**
     * Validates a record against the table's schema.
     * Checks that:
     *  - The number of values equals the number of attributes.
     *  - Each value conforms to the attribute's domain.
     *  - For a primary key attribute, the value is not null or empty.
     */
    private boolean validateRecord(Record record) {
        if (record.getValues().size() != attributes.size()) {
            System.out.println("Error: Number of values does not match table schema.");
            return false;
        }
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attr = attributes.get(i);
            Object value = record.getValue(i);
            
            // Entity Integrity: Primary key must not be null or blank.
            if (attr.isPrimaryKey()) {
                if (value == null || value.toString().trim().isEmpty()) {
                    System.out.println("Error: Primary key attribute '" + attr.getName() + "' cannot be null or empty.");
                    return false;
                }
            }
            
            // Domain Constraints
            if (attr.getDataType() == Attribute.DataType.INTEGER) {
                try {
                    Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    System.out.println("Error: Value for attribute '" + attr.getName() + "' is not a valid integer.");
                    return false;
                }
            } else if (attr.getDataType() == Attribute.DataType.FLOAT) {
                try {
                    Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    System.out.println("Error: Value for attribute '" + attr.getName() + "' is not a valid float.");
                    return false;
                }
            } else if (attr.getDataType() == Attribute.DataType.TEXT) {
                String text = value.toString();
                if (text.length() > 100) {
                    System.out.println("Error: Value for attribute '" + attr.getName() + "' exceeds 100 characters.");
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Inserts a record into the table.
     * Validates the record (domain and entity constraints) and checks for duplicate primary key values.
     */
    public boolean insert(Record record) {
        if (!validateRecord(record)) {
            return false;
        }
        
        if (primaryKey != null && bstIndex != null) {
            int pkIndex = -1;
            for (int i = 0; i < attributes.size(); i++) {
                if (attributes.get(i).getName().equalsIgnoreCase(primaryKey)) {
                    pkIndex = i;
                    break;
                }
            }
            if (pkIndex == -1) {
                System.out.println("Primary key attribute not found in schema.");
                return false;
            }
            Object keyValue = record.getValue(pkIndex);
            if (!(keyValue instanceof Comparable)) {
                System.out.println("Primary key value is not comparable.");
                return false;
            }
            KeyWrapper wrappedKey = new KeyWrapper(keyValue);
            // Check for duplicate key.
            Record existingRecord = bstIndex.search(wrappedKey);
            if (existingRecord != null) {
                System.out.println("Error: Duplicate primary key value: " + keyValue);
                return false;
            }
            bstIndex.insert(wrappedKey, record);
        }
        records.add(record);
        System.out.println("Record inserted into table '" + name + "'.");
        return true;
    }
    
    /**
     * Retrieves records that match the given condition.
     * If the table has a primary key with a BST index, the records are retrieved via an in-order traversal.
     */
    public List<Record> select(String condition) {
        List<Record> result = new ArrayList<>();
        List<Record> searchList;
        if (primaryKey != null && bstIndex != null) {
            searchList = bstIndex.inOrderTraversal();
        } else {
            searchList = records;
        }
        if (condition == null || condition.trim().isEmpty()) {
            return new ArrayList<>(searchList);
        }
        for (Record record : searchList) {
            if (recordMatchesCondition(record, condition)) {
                result.add(record);
            }
        }
        return result;
    }
    
    /**
     * Public helper to check if a record matches the condition.
     */
    public boolean matchesCondition(Record record, String condition) {
        return recordMatchesCondition(record, condition);
    }
    
    // ----------------- Condition Evaluation Helpers -----------------
    
    /**
     * Evaluates a simple condition of the form "AttrName RelOp Constant" for a record.
     */
    private boolean recordMatchesCondition(Record record, String condition) {
        String[] tokens = condition.trim().split("\\s+");
        if (tokens.length < 3) {
            System.out.println("Invalid condition format.");
            return false;
        }
        String attrName = tokens[0];
        String operator = tokens[1];
        String constantValue = tokens[2].replaceAll("^\"|\"$", "");
        
        int attrIndex = -1;
        Attribute attr = null;
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).getName().equalsIgnoreCase(attrName)) {
                attrIndex = i;
                attr = attributes.get(i);
                break;
            }
        }
        if (attrIndex == -1) {
            System.out.println("Attribute " + attrName + " not found in table " + name);
            return false;
        }
        Object recordValue = record.getValue(attrIndex);
        if (attr.getDataType() == Attribute.DataType.INTEGER) {
            try {
                int recVal = Integer.parseInt(recordValue.toString());
                int constVal = Integer.parseInt(constantValue);
                return compareInts(recVal, operator, constVal);
            } catch (NumberFormatException e) {
                System.out.println("Error: Unable to parse integer in condition.");
                return false;
            }
        } else if (attr.getDataType() == Attribute.DataType.FLOAT) {
            try {
                double recVal = Double.parseDouble(recordValue.toString());
                double constVal = Double.parseDouble(constantValue);
                return compareDoubles(recVal, operator, constVal);
            } catch (NumberFormatException e) {
                System.out.println("Error: Unable to parse float in condition.");
                return false;
            }
        } else { // TEXT
            String recVal = recordValue.toString();
            return compareStrings(recVal, operator, constantValue);
        }
    }
    
    private boolean compareInts(int a, String op, int b) {
        switch (op) {
            case "=": case "==":
                return a == b;
            case "!=":
                return a != b;
            case "<":
                return a < b;
            case "<=":
                return a <= b;
            case ">":
                return a > b;
            case ">=":
                return a >= b;
            default:
                System.out.println("Invalid operator: " + op);
                return false;
        }
    }
    
    private boolean compareDoubles(double a, String op, double b) {
        switch (op) {
            case "=": case "==":
                return a == b;
            case "!=":
                return a != b;
            case "<":
                return a < b;
            case "<=":
                return a <= b;
            case ">":
                return a > b;
            case ">=":
                return a >= b;
            default:
                System.out.println("Invalid operator: " + op);
                return false;
        }
    }
    
    private boolean compareStrings(String a, String op, String b) {
        switch (op) {
            case "=": case "==":
                return a.equals(b);
            case "!=":
                return !a.equals(b);
            case "<":
                return a.compareTo(b) < 0;
            case "<=":
                return a.compareTo(b) <= 0;
            case ">":
                return a.compareTo(b) > 0;
            case ">=":
                return a.compareTo(b) >= 0;
            default:
                System.out.println("Invalid operator for strings: " + op);
                return false;
        }
    }
    // ----------------- End of Condition Evaluation Helpers -----------------
    
    /**
     * Updates records in the table that match the given condition.
     * It checks that the new values satisfy domain, entity integrity, and key constraints.
     *
     * @param condition A string condition to select records (e.g., "id = 2").
     * @param updatedValues A Record object containing new values for the update.
     * @return The number of records updated.
     */
    public int update(String condition, Record updatedValues) {
        int updatedCount = 0;
        // Iterate over each record.
        for (Record record : records) {
            if (condition == null || condition.trim().isEmpty() || recordMatchesCondition(record, condition)) {
                List<Object> currentVals = record.getValues();
                List<Attribute> attrs = attributes;
                List<Object> newVals = updatedValues.getValues();
                // For each attribute position provided in updatedValues,
                // update the record only if a new value is present.
                for (int i = 0; i < newVals.size(); i++) {
                    Object newVal = newVals.get(i);
                    if (newVal == null) continue; // Skip update for this attribute
                    
                    Attribute attr = attrs.get(i);
                    
                    // Domain Constraint Check
                    if (attr.getDataType() == Attribute.DataType.INTEGER) {
                        try {
                            Integer.parseInt(newVal.toString());
                        } catch (NumberFormatException e) {
                            System.out.println("Error: New value for attribute '" + attr.getName() + "' is not a valid integer.");
                            continue;
                        }
                    } else if (attr.getDataType() == Attribute.DataType.FLOAT) {
                        try {
                            Double.parseDouble(newVal.toString());
                        } catch (NumberFormatException e) {
                            System.out.println("Error: New value for attribute '" + attr.getName() + "' is not a valid float.");
                            continue;
                        }
                    } else if (attr.getDataType() == Attribute.DataType.TEXT) {
                        String text = newVal.toString();
                        if (text.length() > 100) {
                            System.out.println("Error: New value for attribute '" + attr.getName() + "' exceeds 100 characters.");
                            continue;
                        }
                    }
                    
                    // Entity Integrity & Key Constraint Check for primary key.
                    if (attr.isPrimaryKey()) {
                        if (newVal == null || newVal.toString().trim().isEmpty()) {
                            System.out.println("Error: Primary key '" + attr.getName() + "' cannot be null or empty.");
                            continue;
                        }
                        KeyWrapper newKey = new KeyWrapper(newVal);
                        Record duplicate = bstIndex.search(newKey);
                        if (duplicate != null && duplicate != record) {
                            System.out.println("Error: Duplicate primary key value: " + newVal);
                            continue;
                        }
                        // (Note: A full update would remove the old key and re-insert the new key into the BST.)
                    }
                    
                    // If all checks pass, update the attribute's value.
                    currentVals.set(i, newVal);
                }
                updatedCount++;
            }
        }
        System.out.println(updatedCount + " record(s) updated in table '" + name + "'.");
        return updatedCount;
    }
    
    /**
     * Deletes records from the table that match the given condition.
     * If no condition is provided, deletes all records and resets the BST index.
     *
     * @param condition A string condition.
     * @return The number of records deleted.
     */
    public int delete(String condition) {
        int initialSize = records.size();
        if (condition == null || condition.trim().isEmpty()) {
            if (bstIndex != null)
                bstIndex = new BinarySearchTree<>();
            records.clear();
            System.out.println("All records deleted from table '" + name + "'.");
            return initialSize;
        }
        int deletedCount = 0;
        for (int i = records.size() - 1; i >= 0; i--) {
            Record record = records.get(i);
            if (recordMatchesCondition(record, condition)) {
                records.remove(i);
                deletedCount++;
            }
        }
        System.out.println(deletedCount + " record(s) deleted from table '" + name + "'.");
        return deletedCount;
    }
    
    /**
     * Renames the attributes of the table using the provided new names.
     *
     * @param newNames A list of new attribute names.
     * @return true if renaming is successful; false otherwise.
     */
    public boolean renameAttributes(List<String> newNames) {
        if (newNames.size() != attributes.size()) {
            System.out.println("Error: Number of new names does not match the number of attributes.");
            return false;
        }
        for (int i = 0; i < attributes.size(); i++) {
            attributes.get(i).setName(newNames.get(i));
        }
        System.out.println("Attributes in table '" + name + "' renamed successfully.");
        return true;
    }
    
    // ----------------- Inner Classes -----------------
    
    /**
     * A helper inner class to wrap primary key values for use in the BST.
     */
    private static class KeyWrapper implements Comparable<KeyWrapper>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Object key;
        
        public KeyWrapper(Object key) {
            if (!(key instanceof Comparable)) {
                throw new IllegalArgumentException("Key must implement Comparable.");
            }
            this.key = key;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public int compareTo(KeyWrapper other) {
            return ((Comparable<Object>) this.key).compareTo(other.key);
        }
        
        @Override
        public String toString() {
            return key.toString();
        }
    }
    
    /**
     * Inner class representing an Attribute.
     */
    public static class Attribute implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private DataType dataType;
        private boolean primaryKey;
        
        public enum DataType {
            INTEGER, FLOAT, TEXT
        }
        
        public Attribute(String name, DataType dataType, boolean primaryKey) {
            this.name = name;
            this.dataType = dataType;
            this.primaryKey = primaryKey;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public DataType getDataType() {
            return dataType;
        }
        
        public boolean isPrimaryKey() {
            return primaryKey;
        }
    }
    
    /**
     * Inner class representing a Record (tuple) in the table.
     */
    public static class Record implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Object> values;
        
        public Record(List<Object> values) {
            this.values = values;
        }
        
        public List<Object> getValues() {
            return values;
        }
        
        public Object getValue(int index) {
            return values.get(index);
        }
        
        public void setValue(int index, Object value) {
            values.set(index, value);
        }
    }
}

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Table implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private List<Attribute> attributes;
    private List<Record> records;
    private String primaryKey; // Name of the primary key attribute.
    
    // Uses our KeyWrapper (see below) with a type-safe BST.
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
     * Inserts a record into the table.
     * If a primary key exists, extracts, wraps, and inserts it into the BST index.
     */
    public boolean insert(Record record) {
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
            bstIndex.insert(wrappedKey, record);
        }
        records.add(record);
        System.out.println("Record inserted into table '" + name + "'.");
        return true;
    }
    
    /**
     * Selects records that match the given condition.
     * If the condition is blank, returns a copy of all records.
     */
    public List<Record> select(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            return new ArrayList<>(records);
        } else {
            List<Record> filtered = new ArrayList<>();
            for (Record record : records) {
                if (recordMatchesCondition(record, condition)) {
                    filtered.add(record);
                }
            }
            return filtered;
        }
    }
    
    /**
     * Public helper for other classes to check if a record matches a condition.
     */
    public boolean matchesCondition(Record record, String condition) {
        return recordMatchesCondition(record, condition);
    }
    
    // ----------------- Condition Evaluation Helpers -----------------
    
    /**
     * Evaluates a simple condition of the form "attr operator constant" for a record.
     */
    private boolean recordMatchesCondition(Record record, String condition) {
        String[] tokens = condition.trim().split("\\s+");
        if (tokens.length < 3) {
            System.out.println("Invalid condition format.");
            return false;
        }
        String attrName = tokens[0];
        String operator = tokens[1];
        String constantValue = tokens[2];
        
        // Find the attribute index.
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
        // Remove surrounding quotes from the constant (if any).
        constantValue = constantValue.replaceAll("^\"|\"$", "");
        
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
            case "=":
            case "==":
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
            case "=":
            case "==":
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
            case "=":
            case "==":
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
     * Renames the attributes of the table using the provided new names.
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

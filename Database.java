import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Database implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    // Stores tables as Table objects.
    private Map<String, Table> tables;
    
    public Database(String name) {
        this.name = name;
        this.tables = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void addTable(String tableName, Table table) {
        if (tables.containsKey(tableName)) {
            System.out.println("Error: Table '" + tableName + "' already exists in database '" + name + "'.");
        } else {
            tables.put(tableName, table);
            System.out.println("Table '" + tableName + "' added to database '" + name + "'.");
        }
    }
    
    public Table getTable(String tableName) {
        if (tables.containsKey(tableName))
            return tables.get(tableName);
        else {
            System.out.println("Error: Table '" + tableName + "' does not exist in database '" + name + "'.");
            return null;
        }
    }
    
    public void deleteTable(String tableName) {
        if (tables.containsKey(tableName)) {
            tables.remove(tableName);
            System.out.println("Table '" + tableName + "' deleted from database '" + name + "'.");
        } else {
            System.out.println("Error: Table '" + tableName + "' does not exist in database '" + name + "'.");
        }
    }
    
    public Set<String> listTables() {
        return tables.keySet();
    }
    
    @Override
    public String toString() {
        return "Database{" +
               "name='" + name + '\'' +
               ", tables=" + tables.keySet() +
               '}';
    }
}

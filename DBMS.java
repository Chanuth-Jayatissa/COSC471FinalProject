import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DBMS implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Stores databases as Database objects.
    private Map<String, Database> databases;
    
    // Reference to the current active database.
    private Database currentDatabase;
    
    private String persistenceFile = "dbms_state.ser";
    
    /**
     * Constructor – initializes an empty DBMS and loads persisted state if available.
     */
    public DBMS() {
        databases = new HashMap<>();
        initialize();
    }
    
    /**
     * Loads saved state from the persistence file.
     */
    public void initialize() {
        File file = new File(persistenceFile);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                DBMS state = (DBMS) ois.readObject();
                this.databases = state.getDatabases();
                this.currentDatabase = state.getCurrentDatabase();
                System.out.println("DBMS initialized. Persistent state loaded successfully.");
            } catch (Exception e) {
                System.out.println("Failed to load persistent state: " + e.getMessage());
            }
        } else {
            System.out.println("No persistent state found. Starting with a clean DBMS.");
        }
    }
    
    /**
     * Saves the current state to a file.
     */
    public void saveState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(persistenceFile))) {
            oos.writeObject(this);
            System.out.println("State saved successfully.");
        } catch (IOException e) {
            System.out.println("Error saving state: " + e.getMessage());
        }
    }
    
    /**
     * Executes a DBMS command.
     */
    public void executeCommand(Command command) {
        try {
            command.execute(this);
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
    }
    
    /**
     * Creates a new database.
     */
    public void createDatabase(String dbName) {
        if (databases.containsKey(dbName)) {
            System.out.println("Error: Database '" + dbName + "' already exists.");
        } else {
            databases.put(dbName, new Database(dbName));
            System.out.println("Database '" + dbName + "' created successfully.");
        }
    }
    
    /**
     * Sets the active database.
     */
    public void useDatabase(String dbName) {
        if (databases.containsKey(dbName)) {
            currentDatabase = databases.get(dbName);
            System.out.println("Now using database: '" + dbName + "'.");
        } else {
            System.out.println("Error: Database '" + dbName + "' does not exist.");
        }
    }
    
    /**
     * Returns the set of database names.
     */
    public Set<String> listDatabases() {
        return databases.keySet();
    }
    
    // Getter methods for persistence.
    public Map<String, Database> getDatabases() {
        return databases;
    }
    
    public Database getCurrentDatabase() {
        return currentDatabase;
    }
    
    /**
     * Command interface for DBMS commands.
     */
    public interface Command {
        void execute(DBMS dbms) throws Exception;
    }
}

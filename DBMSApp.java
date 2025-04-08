import java.util.Scanner;

public class DBMSApp {
    public static void main(String[] args) {
        DBMS dbms = new DBMS();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the DBMS. Type your commands (e.g., \"CREATE DATABASE abc\") and type EXIT to quit.");
        
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("EXIT")) {
                dbms.saveState();
                System.out.println("Exiting DBMS. State has been saved.");
                break;
            }
            
            if (input.isEmpty()) {
                continue;
            }
            
            try {
                DBMS.Command command = CommandParser.parse(input);
                dbms.executeCommand(command);
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
        }
        
        scanner.close();
    }
}

import java.util.Scanner;

public class DBMSApp {
    public static void main(String[] args) {
        DBMS dbms = new DBMS();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the DBMS.");
        System.out.println("Enter your commands below (multi-line input allowed, each command ends with a ';'):");

        StringBuilder commandBuilder = new StringBuilder();

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine();

            commandBuilder.append(line).append(" ");


            if (line.trim().endsWith(";")) {
                String command = commandBuilder.toString().trim();

                if (command.endsWith(";")) {
                    command = command.substring(0, command.length() - 1).trim();
                }

                try {
                    DBMS.Command cmd = CommandParser.parse(command);
                    cmd.execute(dbms);
                } catch (Exception e) {
                    System.out.println("Error executing command: " + e.getMessage());
                }

                if (command.equalsIgnoreCase("EXIT")) {
                    break;
                }
                
                commandBuilder.setLength(0);
            }
        }
        
        scanner.close();
    }
}

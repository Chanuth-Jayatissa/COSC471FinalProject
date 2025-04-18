import java.util.Scanner;

public class DBMSApp {
    public static void main(String[] args) {
        DBMS dbms = new DBMS();
        System.out.println("Welcome to the DBMS.");
        System.out.println("Enter your commands below (multi-line input allowed, each command ends with a ';'):");

        // Use try-with-resources to ensure the scanner is closed automatically
        try (Scanner scanner = new Scanner(System.in)) {
            StringBuilder commandBuilder = new StringBuilder();

            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine();

                // Accumulate input
                commandBuilder.append(line).append(" ");
                String allInput = commandBuilder.toString().trim();

                int idx;
                // Process each complete command terminated by a semicolon
                while ((idx = allInput.indexOf(';')) != -1) {
                    String cmdText = allInput.substring(0, idx).trim();
                    if (!cmdText.isEmpty()) {
                        try {
                            DBMS.Command cmd = CommandParser.parse(cmdText);
                            cmd.execute(dbms);
                            if (cmdText.equalsIgnoreCase("EXIT")) {
                                // Exit the application
                                return;
                            }
                        } catch (Exception e) {
                            System.out.println("Error executing command: " + e.getMessage());
                        }
                    }
                    // Remove the processed command and its semicolon
                    allInput = allInput.substring(idx + 1).trim();
                }

                // Preserve any partial command for the next iteration
                commandBuilder.setLength(0);
                commandBuilder.append(allInput).append(" ");
            }
        }
    }
}

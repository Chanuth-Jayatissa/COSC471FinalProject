import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    /**
     * Reads commands from a file – one command per non-empty line.
     */
    public static List<String> readCommands(String fileName) throws IOException {
        List<String> commands = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty())
                    commands.add(line);
            }
        }
        return commands;
    }
    
    /**
     * Writes the output string to a file.
     */
    public static void writeOutput(String fileName, String output) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            bw.write(output);
        }
    }
}

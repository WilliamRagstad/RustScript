import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Scanner;
import java.io.IOException;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 * @version 0.1.0
 * 
 *          The Runner takes a set of input source code files as command
 *          line argument, and interprets them by expression. Even if one
 *          file fails to execute, the rest will.
 *
 */
public class Runner {
    public static void main(String[] args) throws Exception {
        // args = new String[] { "test\\input1.rs" };
        if (args.length == 0) {
            System.out.println("Usage: Runner [file(s)]");
            return;
        }
        Interpreter i;
        for (String file : args) {
            String[] source;
            try {
                source = Files.readString(Paths.get(file)).split("\n");
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            i = new Interpreter(); // New environment for each file.
            try {
                i.evalAll(source); // Discard last the expressions value
            }  catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

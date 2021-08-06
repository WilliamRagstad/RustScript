import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import core.Interpreter;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 * @version 0.1.0
 *
 *          The Runner takes a set of input source code files as command line
 *          argument, and interprets them by expression. Even if one file fails
 *          to execute, the rest will.
 *
 */
public class Runner {
	public static void main(String[] args) throws Exception {
		// args = new String[] { "test\\input4.rs" };
		if (args.length == 0) {
			System.out.println("Usage: Runner [file(s)]");
			return;
		}

	}

	public static void run(String[] files) {
		Interpreter i;
		try {
			i = new Interpreter();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		for (String file : files) {
			File f = new File(file);
			if (!f.exists()) {
				System.out.println("File '" + file + "' does not exist.");
				return;
			} else if (f.isDirectory()) {
				System.out.println("File '" + file + "' is a directory.");
				return;
			} else if (!f.canRead()) {
				System.out.println("File '" + file + "' is not readable.");
				return;
			}
			String source;
			try {
				source = Files.readString(Paths.get(file), StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			try {
				i.evalAll(source); // Discard last the expressions value
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return;
			}
			i.clear(); // New environment for each file.
		}
	}
}

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import core.Interpreter;
import helper.FileHelper;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 *
 *         The Runner takes a set of input source code files as command line
 *         argument, and interprets them by expression. Even if one file fails
 *         to execute, the rest will.
 *
 */
public class Runner {
	public static void main(String[] args) throws Exception {
		// args = new String[] { "test\\input9.2.rs" };
		if (args.length == 0) {
			System.out.println("Usage: Runner [file(s)]");
			return;
		}
		run(Arrays.asList(args));
	}

	public static void run(List<String> files) {
		Interpreter i;
		try {
			i = new Interpreter();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		for (String file : files) {
			String source;
			Path filePath = Paths.get(file);
			try {
				source = FileHelper.readFile(filePath);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			try {
				String p1 = filePath.getParent().toString();
				String p2 = filePath.getParent().normalize().toAbsolutePath().toString();
				i.evalAll(source, p2); // Discard last the expressions value
			} catch (Exception e) {
				System.out.println(e.getMessage());
				return;
			}
			i.clear(); // New environment for each file.
		}
	}
}

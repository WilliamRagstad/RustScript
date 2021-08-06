import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import core.Parser;

public class Linter {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
			System.out.println("Usage: Linter [file(s)]");
			return;
		}
        run(Arrays.asList(args));
    }

    public static void run(List<String> files) {
        System.out.println("Linting " + files.size() + " file(s)...");
        int failed = 0;
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
                Parser.parseExprs(source);
			} catch (Exception e) {
				System.out.println(String.format("""
Syntax error in '%s':
    %s
                """, file, e.getMessage()));
                failed++;
			}
		}
        if (failed == 0) {
            System.out.println("All files passed!");
        } else {
            System.out.println("Found " + failed + " errors.");
        }
	}
}

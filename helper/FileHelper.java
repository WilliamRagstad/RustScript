package helper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHelper {
	public static void validateIsFile(Path filePath) throws IOException {
		File f = filePath.toFile();
		if (!f.exists()) {
			throw new IOException("File '" + f + "' does not exist.");
		} else if (f.isDirectory()) {
			throw new IOException("File '" + f + "' is a directory.");
		} else if (!f.canRead()) {
			throw new IOException("File '" + f + "' is not readable.");
		}
	}

	public static String readFile(Path filePath) throws IOException {
		validateIsFile(filePath);
		return Files.readString(filePath, StandardCharsets.UTF_8);
	}
}

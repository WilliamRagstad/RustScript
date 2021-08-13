import java.util.Scanner;
import java.util.function.Consumer;

import core.Interpreter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>, William Rågstad
 *         <william.ragstad@gmail.com>
 *
 *         The REPL program lets you interact with the RustScript programming
 *         language and run commands as you go.
 */
public class Repl {
	public static void main(String[] args) {
		currentDirectory = System.getProperty("user.dir");
		run();
	}

	private static String currentDirectory;
	private static HashMap<String, Consumer<String[]>> commands = new HashMap<String, Consumer<String[]>>() {
		{
			put("pwd", (args) -> {
				System.out.println(currentDirectory);
			});
			put("cd", (args) -> {
				if (args.length > 1) {
					System.out.println("cd: too many arguments");
				} else if (args.length == 1) {
					Path to = Paths.get(currentDirectory).resolve(args[0]);
					if (to.toFile().exists() && Files.isDirectory(to)) {
						currentDirectory = to.normalize().toAbsolutePath().toString();
					} else {
						System.out.println("cd: no such directory");
					}
				} else {
					System.out.println("cd: missing operand");
				}
			});
		}
	};

	private static boolean tryRunCommand(String expr) {
		ArrayList<String> args = new ArrayList<>(Arrays.asList(expr.split(" ")));
		String command = args.remove(0);
		if (commands.containsKey(command)) {
			commands.get(command).accept(args.toArray(new String[args.size()]));
			return true;
		}
		return false;
	}

	public static void run() {
		Interpreter i;
		try {
			i = new Interpreter();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return;
		}
		Scanner sc = new Scanner(System.in, getSupportedCharset());
		for (;;) {
			try {
				System.out.print("> ");
				String expr = sc.nextLine();
				if (!tryRunCommand(expr)) {
					i.executeAll(expr, currentDirectory);
				}
			} catch (NoSuchElementException e) {
				System.out.println(e.getMessage());
				sc.close();
				break;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	private static String getSupportedCharset() {
		switch (System.getProperty("os.name")) {
			case "Windows 10":
				return "Cp850";
			default:
				return "UTF-8";
		}
	}
}

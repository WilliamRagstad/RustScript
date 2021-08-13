import java.util.Scanner;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;

import core.Interpreter;

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
				System.out.println("pwd: " + currentDirectory);
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
			put("ls", (args) -> {
				if (args.length > 0) {
					System.out.println("ls: no arguments expected");
				} else {
					File currentFile = new File(currentDirectory);
					Stack<String> files = new Stack<String>();
					Supplier<String> file = () -> {
						if (files.isEmpty())
							return "";
						String f = files.pop();
						return f == null ? "" : f;
					};
					for (File f : currentFile.listFiles()) {
						if (f.isDirectory()) {
							files.push(f.getName() + "/");
						} else {
							files.push(f.getName());
						}
						if (files.size() >= 3) {
							System.out.printf("%-22s%-22s%-22s\n", file.get(), file.get(), file.get());
						}
					}
					if (!files.isEmpty()) {
						// Push the last three files off the stack
						System.out.printf("%-22s%-22s%-22s\n", file.get(), file.get(), file.get());
					}
				}
			});
		}
	};

	private static boolean tryRunCommand(String expr) {
		ArrayList<String> args = new ArrayList<>(Arrays.asList(expr.trim().split(" ")));
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

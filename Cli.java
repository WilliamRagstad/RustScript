import java.util.List;
import java.util.Arrays;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 *
 *          The CLI is a simple command line interface for the RustScript
 *          language. It provides a simple way to run RustScript script files
 *          and interact in a playful way using the REPL mode. Sometime in the
 *          future, this might be extended to include a compiler to other
 *          languages like JavaScript, C++ or Python.
 */
public class Cli {
	private static final String VERSION = "2.2.0";
	private static final String DESCRIPTION = "A command line interface tool for the RustScript language.";
	private static final String COPYRIGHT = "Copyright (c) 2021 William Rågstad";
	private static final String HELP = String.format("""
			RustScript CLI version %s.
			%s

			Usage: rsc (options) (files)

			Options:
				-h, --help
					Prints this help message.
				-v, --version
					Prints the version of the program.
				-r, --repl
					Starts the REPL mode.
				-l, --lint [files]
					Lints the given files.
				-c, --compile [files]          (Not implemented)
					Compiles the given files.

			Execute scripts: rsc [files]
				Interprets the given script files one at a time.

			%s""", VERSION, DESCRIPTION, COPYRIGHT);

	public static void main(String[] args) {
		List<String> options = Arrays.asList(args).stream()
			.filter(arg -> arg.startsWith("-"))
			.map(String::toLowerCase).toList();
		List<String> files = Arrays.asList(args).stream()
			.filter(arg -> !arg.startsWith("-")).toList();
		if (options.contains("--help") || options.contains("-h")) {
			System.out.println(HELP);
		} else if (options.contains("--version") || options.contains("-v")) {
			System.out.println(VERSION);
		} else if (options.contains("--repl") || options.contains("-r")) {
			Repl.run();
		} else if (options.contains("--lint") || options.contains("-l")) {
			if (files.isEmpty()) {
				System.out.println("No files given.");
			} else {
				Linter.run(files);
			}
		} else {
			if (args.length > 0) {
				if (options.size() == 1) {
					System.out.println(String.format("Error: Unknown option '%s'", options.get(0)));
				} else if (options.size() > 1) {
					System.out.println(String.format("Error: Unknown options '%s'", String.join("', '", options)));
				} else {
					// All args are files
					Runner.run(files);
				}
			} else {
				System.out.println(HELP);
			}
		}
		// TODO: Add linting and compilation
	}
}

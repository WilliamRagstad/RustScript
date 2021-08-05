import java.util.List;
import java.util.Arrays;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 * @version 0.1.0
 *
 *          The CLI is a simple command line interface for the RustScript
 *          language. It provides a simple way to run RustScript script files
 *          and interact in a playful way using the REPL mode. Sometime in the
 *          future, this might be extended to include a compiler to other
 *          languages like JavaScript, C++ or Python.
 */
public class Cli {
	private static final String VERSION = "0.1.0";
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
				-c, --compile [files]          (Not implemented)
					Compiles the given files.
				-l, --lint [files]             (Not implemented)
					Lints the given files.

			Execute scripts: rsc [files]
				Interprets the given script files one at a time.

			%s""", VERSION, DESCRIPTION, COPYRIGHT);

	public static void main(String[] args) {
		List<String> argsList = Arrays.asList(args).stream()
				.map((String option) -> option.startsWith("-") ? option.toLowerCase() : option).toList();
		if (argsList.contains("--help") || argsList.contains("-h")) {
			System.out.println(HELP);
		} else if (argsList.contains("--version") || argsList.contains("-v")) {
			System.out.println(VERSION);
		} else if (argsList.contains("--repl") || argsList.contains("-r")) {
			Repl.run();
		} else {
			if (argsList.size() > 0) {
				if (argsList.stream().anyMatch((String option) -> option.startsWith("-"))) {
					// Unknown option found
					System.out.println("Unknown option");
				} else {
					// All args are files
					Runner.run(args);
				}
			} else {
				System.out.println(HELP);
			}
		}
		// TODO: Add linting and compilation
	}
}

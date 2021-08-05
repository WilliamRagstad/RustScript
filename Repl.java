import java.util.Scanner;
import java.util.NoSuchElementException;

/**
 * @author William Rågstad <william.ragstad@gmail.com>
 * @version 0.1.0
 *
 *          The REPL program lets you interact with the RustScript programming
 *          language and run commands as you go.
 */
public class Repl {
	public static void main(String[] args) throws Exception {
		Interpreter i = new Interpreter();
		Scanner sc = new Scanner(System.in, getSupportedCharset());
		for (;;) {
			try {
				System.out.print("> ");
				String expr = sc.nextLine();
				i.executeAll(expr);
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

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
        Scanner sc = new Scanner(System.in); // Does not support unicode (e.g å, ä, ö)
	public static void main(String[] args) throws Exception {
		Interpreter i = new Interpreter();
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
}

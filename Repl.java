import java.util.Scanner;
import java.util.NoSuchElementException;

public class Repl {
    public static void main(String[] args) throws Exception {
        Interpreter i = new Interpreter();
        Scanner sc = new Scanner(System.in); // Does not support unicode (e.g å, ä, ö)
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

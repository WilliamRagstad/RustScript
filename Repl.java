import java.util.Scanner;
import java.util.NoSuchElementException;

public class Repl {
    public static void main(String[] args) throws Exception {
        Interpreter i = new Interpreter();
        Scanner sc = new Scanner(System.in);
        for (;;) {
            try {
                System.out.print("> ");
                String expr = sc.nextLine();
                i.execute(expr);
            } catch (NoSuchElementException e) {
                sc.close();
                break;
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

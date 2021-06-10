import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 * 
 *          Because this is an expression based language we don't need to deal
 *          with complicated scoping and whatnot. This keeps the interpreter
 *          very simple.
 *
 */
public class Interpreter {
    HashMap<String, Atom> globals;

    public Interpreter() throws Exception {
        globals = new HashMap<>();

        // small standard library
        execute("let range = fn(a, b) => if (a == b - 1) then ([a]) else ([a] + range(a + 1, b))");
        execute("let fmap = fn(f, ls) => if (ls) then ([f(^ls)] + fmap(f, $ls)) else ([])");
        execute("let filter = fn(f, ls) => if (ls) then (if (f(^ls)) then ([^ls] + filter(f, $ls)) else (filter(f, $ls))) else ([])");
        execute("let fold = fn(f, acc, ls) => if (ls) then (fold(f, f(acc, ^ls), $ls)) else (acc)");
        execute("let sum = fn(ls) => fold(fn (a, b) => a + b, 0, ls)");
        execute("let product = fn(ls) => fold(fn (a, b) => a * b, 1, ls)");
        execute("let reverse = fn(ls) => fold(fn (rs, el) => [el] + rs, [], ls)");
    }

    public Atom eval(String expr) throws Exception {
        return Parser.parseExpr(expr).eval(globals);
    }

    public void execute(String expr) throws Exception {
        Atom res = eval(expr);
        if (!(res instanceof Atom.Unit)) {
            System.out.println(res.toString());
        }
    }
    
    public ArrayList<Atom> evalAll(String expr) throws Exception {
        ArrayList<Expr> expressions = new ArrayList<>();
        Expr tmp;
        while(true) {
            expr = expr.trim();
            if (expr.equals("")) break;
            tmp = Parser.parseExpr(expr);
            expressions.add(tmp);
            if (tmp.endIndex == -1) break;
            expr = expr.substring(tmp.endIndex);
        }

        ArrayList<Atom> results = new ArrayList<>();
        for (Expr e : expressions) {
            results.add(e.eval(globals));
        }
        return results;
    }

    public void executeAll(String expr) throws Exception {
        ArrayList<Atom> res = evalAll(expr);
        for (Atom r : res) {
            if (!(r instanceof Atom.Unit)) {
                System.out.println(r.toString());
            }
        }
    }
}

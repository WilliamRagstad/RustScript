import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Function;

import core.*;

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
    HashMap<String, ProgramFunction> program; // Built in system functions

    public Interpreter() throws Exception {
        globals = new HashMap<>();
        program = new HashMap<>();

        // load built-ins
        loadProgram();

        // small standard library
        execute("let range = fn(a, b) => if (a == b - 1) then ([a]) else ([a] + range(a + 1, b))");
        execute("let fmap = fn(f, ls) => if (ls) then ([f(^ls)] + fmap(f, $ls)) else ([])");
        execute("let filter = fn(f, ls) => if (ls) then (if (f(^ls)) then ([^ls] + filter(f, $ls)) else (filter(f, $ls))) else ([])");
        execute("let fold = fn(f, acc, ls) => if (ls) then (fold(f, f(acc, ^ls), $ls)) else (acc)");
        execute("let sum = fn(ls) => fold(fn (a, b) => a + b, 0, ls)");
        execute("let product = fn(ls) => fold(fn (a, b) => a * b, 1, ls)");
        execute("let reverse = fn(ls) => fold(fn (rs, el) => [el] + rs, [], ls)");
    }

    private void loadProgram() {
        // Helper functions
        Function2<ArrayList<Expr>, Character, Atom> printFunc = (expressions, suffix) -> {
            for (int i = 0; i < expressions.size(); i++) {
                Atom val = expressions.get(i).eval(globals, program);
                if (val instanceof Atom.Str) System.out.print(((Atom.Str)val).getStringValue());
                else if (val instanceof Atom.Char) System.out.print(((Atom.Char)val).getCharValue());
                else System.out.print(val.toString());
                if (i < expressions.size() - 1) System.out.print(' ');
            }
            if (suffix != null) System.out.print(suffix);
            return new Atom.Unit();
        };
        Consumer2<ArrayList<Expr>, Integer> expect = (args, n) -> {
            if (args.size() != n) throw new Exception(String.format("Expected %d argument to call of function input, got %d", n, args.size()));
        };
        // Program built-ins
        program.put("print", (expressions) -> printFunc.apply(expressions, null));
        program.put("println", (expressions) -> printFunc.apply(expressions, '\n'));
        program.put("input", (expressions) -> {
            expect.apply(expressions, 1);
            Atom textAtom = expressions.get(0).eval(globals, program);
            if (!(textAtom instanceof Atom.Str || textAtom instanceof Atom.Char)) throw new Exception(String.format("Can't coerce %s to a string or char", textAtom.toString()));
            String textVal = null;
            if (textAtom instanceof Atom.Str) textVal = ((Atom.Str)textAtom).getStringValue();
            if (textAtom instanceof Atom.Char) textVal = ""+((Atom.Char)textAtom).val;
            System.out.print(textVal);
            Scanner in = new Scanner(System.in);
            String inputVal = in.nextLine();
            return new Atom.Str(inputVal);
        });
        program.put("typeof", (expressions) -> {
            expect.apply(expressions, 1);
            return new Atom.Str(expressions.get(0).eval(globals, program).getClass().getSimpleName());
        });
    }

    public Atom eval(String expr) throws Exception {
        return Parser.parseExpr(expr).eval(globals, program);
    }

    public void execute(String expr) throws Exception {
        Atom res = eval(expr);
        if (!(res instanceof Atom.Unit)) {
            System.out.println(res.toString());
        }
    }
    
    // public ArrayList<Atom> evalAll(String expr) throws Exception {
    //     ArrayList<Expr> expressions = new ArrayList<>();
    //     Expr tmp;
    //     while(true) {
    //         expr = expr.trim();
    //         if (expr.equals("")) break;
    //         tmp = Parser.parseExpr(expr);
    //         expressions.add(tmp);
    //         if (tmp.endIndex == -1) break;
    //         expr = expr.substring(tmp.endIndex);
    //     }

    //     ArrayList<Atom> results = new ArrayList<>();
    //     for (Expr e : expressions) {
    //         results.add(e.eval(globals, program));
    //     }
    //     return results;
    // }

    /**
     * This method evaluates all expressions and returns the last expressions value.
     * Can be used for closures and running script files (and discarding the returned result).
     * @param exprs Expressions to evaluate
     * @throws Exception
     */
    public Atom evalAll(String[] exprs) throws Exception {
        for (int i = 0; i < exprs.length; i++) {
            String expr = exprs[i].trim();
            if (expr == "") continue;
            if (i == exprs.length - 1) return eval(expr); // Return if last, this is to skip overwriting a temp var.
            eval(expr); // Otherwise don't return the result
        }
        return new Atom.Unit(); // If exprs array is empty, return the unit.
    }
}

@FunctionalInterface
interface Function2<One, Two, Return> {
    public Return apply(One one, Two two) throws Exception;
}
@FunctionalInterface
interface Consumer2<One, Two> {
    public void apply(One one, Two two) throws Exception;
}
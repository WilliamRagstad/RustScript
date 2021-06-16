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

        // small standard library
        execute("let range = fn(a, b) => if (a == b - 1) then ([a]) else ([a] + range(a + 1, b))");
        execute("let fmap = fn(f, ls) => if (ls) then ([f(^ls)] + fmap(f, $ls)) else ([])");
        execute("let filter = fn(f, ls) => if (ls) then (if (f(^ls)) then ([^ls] + filter(f, $ls)) else (filter(f, $ls))) else ([])");
        execute("let fold = fn(f, acc, ls) => if (ls) then (fold(f, f(acc, ^ls), $ls)) else (acc)");
        execute("let sum = fn(ls) => fold(fn (a, b) => a + b, 0, ls)");
        execute("let product = fn(ls) => fold(fn (a, b) => a * b, 1, ls)");
        execute("let reverse = fn(ls) => fold(fn (rs, el) => [el] + rs, [], ls)");
        
        // load built-ins
        loadProgram();
        // wrappers for built-ins
        execute("let print = fn(s) => _print_(s)");
        execute("let println = fn(s) => _println_(s)");
        execute("let input = fn(s) => _input_(s)");
        execute("let typeof = fn(e) => _typeof_(e)");
    }

    private void loadProgram() {
        // Helper functions
        Consumer3<ArrayList<Atom>, Integer, String> expect = (args, n, name) -> {
            if (args.size() != n) throw new Exception(String.format("Expected %d argument to call of function %s, got %d", n, name, args.size()));
        };
        Function2<ArrayList<Atom>, Character, Atom> printFunc = (args, suffix) -> {
            for (int i = 0; i < args.size(); i++) {
                Atom val = args.get(i); // args.get(i).eval(globals, program)
                if (val instanceof Atom.Str) System.out.print(((Atom.Str)val).getStringValue());
                else if (val instanceof Atom.Char) System.out.print(((Atom.Char)val).getCharValue());
                else System.out.print(val.toString());
                if (i < args.size() - 1) System.out.print(' ');
            }
            if (suffix != null) System.out.print(suffix);
            return new Atom.Unit();
        };
        // Program built-ins
        program.put("_print_", (args) -> printFunc.apply(args, null));
        program.put("_println_", (args) -> printFunc.apply(args, '\n'));
        program.put("_input_", (args) -> {
            expect.apply(args, 1, "input");
            Atom textAtom = args.get(0); // args.get(0).eval(globals, program)
            if (!(textAtom instanceof Atom.Str || textAtom instanceof Atom.Char)) throw new Exception(String.format("Can't coerce %s to a string or char", textAtom.toString()));
            String textVal = null;
            if (textAtom instanceof Atom.Str) textVal = ((Atom.Str)textAtom).getStringValue();
            if (textAtom instanceof Atom.Char) textVal = ""+((Atom.Char)textAtom).val;
            System.out.print(textVal);
            Scanner in = new Scanner(System.in);
            String inputVal = in.nextLine();
            return new Atom.Str(inputVal);
        });
        program.put("_typeof_", (args) -> {
            expect.apply(args, 1, "typeof");
            return new Atom.Str(args.get(0).getClass().getSimpleName());
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
    public Atom evalAll(String program) throws Exception {
        String[] exprs = program.split("\n");   // Use both ; and \n to separate expressions, todo: Rigorous implementation
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
interface Consumer3<One, Two, Three> {
    public void apply(One one, Two two, Three three) throws Exception;
}
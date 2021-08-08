package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Function;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>, William Rågstad <william.ragstad@gmail.com>
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
	}

	private String GenerateKernelName(String functionName) {
		int kid = (int) (Math.random() * 1000);
		return "kernel" + kid + "_" + functionName;
	}

	private void loadProgram() throws Exception {
		// Helper functions
		Consumer3<ArrayList<Atom>, Integer, String> expectArgs = (args, n, name) -> {
			if (args.size() != n)
				throw new Exception(
						String.format("Expected %d argument to call of function %s, got %d", n, name, args.size()));
		};
		Consumer3<Atom, Class<?>, String> expectType = (arg, expected, name) -> {
			if (arg.getClass() != expected)
				throw new Exception(
						String.format("Type missmatch! Invalid argument %s to function %s, is not of type %s",
								arg.toString(), name, expected.getSimpleName()));
		};
		// Load program built-ins
		String print = GenerateKernelName("print");
		String input = GenerateKernelName("input");
		String typeof = GenerateKernelName("typeof");
		String upper = GenerateKernelName("upper");
		String lower = GenerateKernelName("lower");
		String round = GenerateKernelName("round");
		String floor = GenerateKernelName("floor");
		String ceil = GenerateKernelName("ceil");
		String substr = GenerateKernelName("substr");
		String parseInt = GenerateKernelName("parseInt");
		String parseBool = GenerateKernelName("parseBool");
		// TODO: Allow print functions to accept any number of arguments
		program.put(print, (args) -> {
			expectArgs.apply(args, 1, "print");
			Atom val = args.get(0); // eval?
			if (val instanceof Atom.Str)
				System.out.print(((Atom.Str) val).getStringValue(false));
			else if (val instanceof Atom.Char)
				System.out.print(((Atom.Char) val).getCharValue());
			else
				System.out.print(val.toString());
			return new Atom.Unit();
		});
		program.put(input, (args) -> {
			expectArgs.apply(args, 1, "input");
			Atom textAtom = args.get(0); // args.get(0).eval(globals, program)
			if (!(textAtom instanceof Atom.Str || textAtom instanceof Atom.Char))
				throw new Exception(String.format("Can't coerce %s to a string or char", textAtom.toString()));
			String textVal = null;
			if (textAtom instanceof Atom.Str)
				textVal = ((Atom.Str) textAtom).getStringValue(false);
			if (textAtom instanceof Atom.Char)
				textVal = "" + ((Atom.Char) textAtom).val;
			System.out.print(textVal);
			Scanner in = new Scanner(System.in);
			String inputVal = in.nextLine();
			return new Atom.Str(inputVal);
		});
		program.put(typeof, (args) -> {
			expectArgs.apply(args, 1, "typeof");
			return new Atom.Str(args.get(0).getClass().getSimpleName());
		});
		program.put(upper, (args) -> {
			expectArgs.apply(args, 1, "upper");
			expectType.apply(args.get(0), Atom.Str.class, "upper");
			return new Atom.Str(((Atom.Str) args.get(0)).getStringValue(false).toUpperCase());
		});
		program.put(lower, (args) -> {
			expectArgs.apply(args, 1, "lower");
			expectType.apply(args.get(0), Atom.Str.class, "lower");
			return new Atom.Str(((Atom.Str) args.get(0)).getStringValue(false).toLowerCase());
		});
		program.put(round, (args) -> {
			expectArgs.apply(args, 1, "round");
			expectType.apply(args.get(0), Atom.Float.class, "round");
			return new Atom.Integer((int) Math.round(((Atom.Float) args.get(0)).val));
		});
		program.put(floor, (args) -> {
			expectArgs.apply(args, 1, "floor");
			expectType.apply(args.get(0), Atom.Float.class, "floor");
			return new Atom.Integer((int)((Atom.Float) args.get(0)).val);
		});
		program.put(ceil, (args) -> {
			expectArgs.apply(args, 1, "ceil");
			expectType.apply(args.get(0), Atom.Float.class, "ceil");
			return new Atom.Integer((int) Math.ceil(((Atom.Float) args.get(0)).val));
		});
		program.put(substr, (args) -> {
			expectArgs.apply(args, 3, "substr");
			expectType.apply(args.get(0), Atom.Str.class, "substr");
			expectType.apply(args.get(1), Atom.Integer.class, "substr");
			expectType.apply(args.get(2), Atom.Integer.class, "substr");
			int start = ((Atom.Integer) args.get(1)).val;
			int end = ((Atom.Integer) args.get(2)).val;
			return new Atom.Str(((Atom.Str) args.get(0)).getStringValue(false).substring(start, end));
		});
		// Parsing
		program.put(parseInt, (args) -> {
			expectArgs.apply(args, 1, "parseInt");
			expectType.apply(args.get(0), Atom.Str.class, "parseInt");
			try {
				return new Atom.Integer(Integer.parseInt(((Atom.Str) args.get(0)).getStringValue(false)));
			} catch (Exception e) {
				return new Atom.Unit();
			}
		});
		program.put(parseBool, (args) -> {
			expectArgs.apply(args, 1, "parseBool");
			expectType.apply(args.get(0), Atom.Str.class, "parseBool");
			String val = ((Atom.Str) args.get(0)).getStringValue(false).trim().toLowerCase();
			if (val.equals("true"))
				return new Atom.Bool(true);
			else if (val.equals("false"))
				return new Atom.Bool(false);
			else
				return new Atom.Unit();
		});
		// wrappers for built-ins
		execute("let print = fn(s) => " + print + "(str(s))");
		execute("var print = fn(s1, s2) => print(str(s1) + \" \" + s2)");
		execute("var print = fn(s1, s2, s3) => print(str(s1) + \" \" + s2, s3)");
		execute("let println = fn(s) => " + print + "(str(s) + '\\n')");
		execute("var println = fn(s1, s2) => println(str(s1) + \" \" + s2)");
		execute("var println = fn(s1, s2, s3) => println(str(s1) + \" \" + s2, s3)");
		execute("let input = fn(s) => " + input + "(s)");
		execute("let typeof = fn(e) => " + typeof + "(e)");
		execute("let upper = fn(s) => " + upper + "(s)");
		execute("let lower = fn(s) => " + lower + "(s)");
		execute("let round = fn(s) => " + round + "(s)");
		execute("let floor = fn(s) => " + floor + "(s)");
		execute("let ceil = fn(s) => " + ceil + "(s)");
		execute("let str = fn(obj) => \"\" + obj");
		execute("let substr = fn(s, b, e) => " + substr + "(s, b, e)");
		execute("let parseInt = fn(s) => " + parseInt + "(s)");
		execute("let parseBool = fn(s) => " + parseBool + "(s)");

		// small standard library
		execute("let range = fn(a, b) => if (a == b - 1) then ([a]) else ([a] + range(a + 1, b))");
		execute("let fmap = fn(f, ls) => if (ls) then ([f(^ls)] + fmap(f, $ls)) else ([])");
		execute("let filter = fn(f, ls) => if (ls) then (if (f(^ls)) then ([^ls] + filter(f, $ls)) else (filter(f, $ls))) else ([])");
		execute("let fold = fn(f, acc, ls) => if (ls) then (fold(f, f(acc, ^ls), $ls)) else (acc)");
		execute("let sum = fn(ls) => fold(fn (a, b) => a + b, 0, ls)");
		execute("let product = fn(ls) => fold(fn (a, b) => a * b, 1, ls)");
		execute("let reverse = fn(ls) => fold(fn (rs, el) => [el] + rs, [], ls)");
		execute("let seq = fn(ls) => ^reverse(ls)"); // Return the last element of the list, which is evaluated from
		// first to last.
		execute("let has = fn(val) => typeof(val) != \"Unit\"");
	}

	/**
	 * Clean the interpreter state and start fresh. The interpreter can then be
	 * reused for running a new program.
	 */
	public void clear() {
		globals.clear();
		program.clear();
		// load built-ins
		try {
			loadProgram();
		} catch (Exception e) {
			// Exceptions cannot be thrown from the interpreter if we have gotten to this
			// point.
			System.out.println(e.getMessage());
		}
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

	/**
	 * This method evaluates all expressions and returns the last expressions value.
	 * Can be used for closures and running script files (and discarding the
	 * returned result).
	 *
	 * @param exprs Expressions to evaluate
	 * @throws Exception
	 */
	public Atom[] evalAll(String program) throws Exception {
		Expr[] exprs = Parser.parseExprs(program); // Use both ; and \n to separate expressions, todo: Rigorous
		// implementation
		ArrayList<Atom> results = new ArrayList<>();
		for (int i = 0; i < exprs.length; i++) {
			results.add(exprs[i].eval(this.globals, this.program));
		}
		Atom[] ret = new Atom[results.size()];
		ret = results.toArray(ret);
		return ret;
	}

	public void executeAll(String program) throws Exception {
		Atom[] res = evalAll(program);
		if (res.length == 0)
			return;
		Atom lst = res[res.length - 1];
		if (!(lst instanceof Atom.Unit)) {
			System.out.println(lst.toString());
		}
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

@FunctionalInterface
interface Consumer3<One, Two, Three> {
	public void apply(One one, Two two, Three three) throws Exception;
}

package core;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import core.Atom.Lambda.LambdaVariation;
import core.formatting.EscapeSequence;
import helper.FileHelper;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>, William Rågstad
 *         <william.ragstad@gmail.com>
 *
 *         An expression, the AST of this language.
 *
 *         <p>
 *         Because this is a functional expression based language, there are no
 *         statements, only expressions. In other words, everything returns
 *         something, even if it's just the unit type.
 *         </p>
 */
public abstract class Expr {
	public int startIndex, endIndex;

	public abstract Atom eval(Scope scope) throws Exception;

	public Expr(int startIndex, int endIndex) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public static class AtomicExpr extends Expr {
		Atom val;

		public Atom eval(Scope scope) throws Exception {
			if (val instanceof Atom.Ident) {
				Atom.Ident v = (Atom.Ident) val;
				Atom res = scope.get(v.name);
				if (res == null) {
					throw new Exception(String.format("Tried to access nonexistent variable %s", v.toString()));
				}
				return res;
			} else if (val instanceof Atom.IdentList) {
				Atom.IdentList v = (Atom.IdentList) val;
				Atom res = scope.find(v.getIdentifiers());
				if (res == null) {
					throw new Exception(String.format("Tried to access nonexistent variable %s", v.toString()));
				}
				return res;
			} else if (val instanceof Atom.List && !(val instanceof Atom.Str)) {
				Atom.List ls = (Atom.List) val;
				ArrayList<Expr> nls = new ArrayList<>();
				for (Expr expr : ls.list) {
					nls.add(new AtomicExpr(expr.eval(scope)));
				}
				Atom.List result = new Atom.List(nls);
				if (result.isCharArray())
					return new Atom.Str(result.getStringValue(false));
				return result;
			} else if (val instanceof Atom.Lambda) {
				((Atom.Lambda) val).setScope(scope);
				return val;
			} else {
				return val;
			}
		}

		public AtomicExpr(Atom val, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.val = val;
		}

		public AtomicExpr(Atom val) {
			super(-1, -1);
			this.val = val;
		}

		public String toString() {
			return val.toString();
		}
	}

	public static class PrefixExpr extends Expr {
		PrefixOp op;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			return switch (op) {
				case Negate -> rhs.eval(scope).negate();
				case Head -> rhs.eval(scope).head(scope);
				case Tail -> rhs.eval(scope).tail();
			};
		}

		public PrefixExpr(PrefixOp op, Expr rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.op = op;
			this.rhs = rhs;
		}

		public PrefixExpr(PrefixOp op, Expr rhs) {
			super(-1, -1);
			this.op = op;
			this.rhs = rhs;
		}

		public String toString() {
			return String.format("%s (%s)", op.toString(), rhs.toString());
		}
	}

	public static class BinaryExpr extends Expr {
		BinOp op;
		Expr lhs;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			return switch (op) {
				case Add -> lhs.eval(scope).add(rhs.eval(scope));
				case Sub -> lhs.eval(scope).sub(rhs.eval(scope));
				case Mul -> lhs.eval(scope).mul(rhs.eval(scope));
				case Div -> lhs.eval(scope).div(rhs.eval(scope));
				case Mod -> lhs.eval(scope).mod(rhs.eval(scope));
				case LT -> lhs.eval(scope).lt(rhs.eval(scope));
				case GT -> lhs.eval(scope).gt(rhs.eval(scope));
				case EQ -> lhs.eval(scope).eq(rhs.eval(scope), scope);
				case NEQ -> lhs.eval(scope).eq(rhs.eval(scope), scope).negate();
				case And -> lhs.eval(scope).and(rhs.eval(scope));
				case Or -> lhs.eval(scope).or(rhs.eval(scope));
			};
		}

		public BinaryExpr(BinOp op, Expr lhs, Expr rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.op = op;
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public BinaryExpr(BinOp op, Expr lhs, Expr rhs) {
			super(-1, -1);
			this.op = op;
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public BinaryExpr(BinOp op, Atom lhs, Atom rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.op = op;
			this.lhs = new AtomicExpr(lhs);
			this.rhs = new AtomicExpr(rhs);
		}

		public BinaryExpr(BinOp op, Atom lhs, Atom rhs) {
			super(-1, -1);
			this.op = op;
			this.lhs = new AtomicExpr(lhs);
			this.rhs = new AtomicExpr(rhs);
		}

		public String toString() {
			return String.format("%s, (%s, %s)", op.toString(), lhs.toString(), rhs.toString());
		}
	}

	public static class BlockExpr extends Expr {
		ArrayList<Expr> exprs;

		public Atom eval(Scope scope) throws Exception {
			Scope blockScope = scope.deriveNew("Block");
			Atom result = new Atom.Unit();
			for (Expr expr : exprs) {
				result = expr.eval(blockScope);
			}
			return result;
		}

		public BlockExpr(ArrayList<Expr> exprs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.exprs = exprs;
		}

		public BlockExpr(ArrayList<Expr> exprs) {
			super(-1, -1);
			this.exprs = exprs;
		}

		public ArrayList<Expr> getExprs() {
			return exprs;
		}

		public String toString() {
			return String.format("{\n\t%s\n}", String.join(";\n\t", exprs.stream().map(Expr::toString).toList()));
		}
	}

	public static class IfExpr extends Expr {
		Expr cond;
		Expr lhs;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			Atom condVal = cond.eval(scope);
			if (condVal.isTruthy()) {
				return lhs.eval(scope);
			} else {
				return rhs.eval(scope);
			}
		}

		public IfExpr(Expr cond, Expr lhs, Expr rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.cond = cond;
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public IfExpr(Expr cond, Expr lhs, Expr rhs) {
			super(-1, -1);
			this.cond = cond;
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public String toString() {
			return String.format("if (%s) then (%s) else (%s)", cond.toString(), lhs.toString(), rhs.toString());
		}
	}

	public static class MatchExpr extends Expr {
		Expr value;
		ArrayList<MatchCaseExpr> cases;

		public Atom eval(Scope scope) throws Exception {
			for (MatchCaseExpr matchCase : cases) {
				Atom.MatchCaseResult matchCaseResult = (Atom.MatchCaseResult) matchCase.eval(scope);
				if (matchCaseResult.isMatch()) {
					return matchCaseResult.getClauseValue();
				}
			}
			throw new Exception("No match found for value: " + value.toString());
		}

		public MatchExpr(Expr value, ArrayList<MatchCaseExpr> cases, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.value = value;
			this.cases = cases;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("match (%s)", value.toString()));
			for (MatchCaseExpr matchCase : cases) {
				sb.append(String.format("   %s", matchCase.toString()));
			}
			return sb.toString();
		}
	}

	public static class MatchCaseExpr extends Expr {
		Expr value;
		String pattern;
		Expr constraint;
		Expr clause;

		public Atom eval(Scope scope) throws Exception {
			Scope clausScope = scope.deriveNew("Match " + value.toString());
			clausScope.set(pattern, value.eval(scope));
			if (constraint == null) {
				return Atom.MatchCaseResult.matched(clause.eval(clausScope));
			} else {
				Atom constraintResult = constraint.eval(clausScope);
				if (constraintResult.isTruthy()) {
					return Atom.MatchCaseResult.matched(clause.eval(clausScope));
				} else {
					return Atom.MatchCaseResult.noMatch();
				}
			}
		}

		public MatchCaseExpr(Expr value, String pattern, Expr constraint, Expr clause, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.value = value;
			this.pattern = pattern;
			this.constraint = constraint;
			this.clause = clause;
		}

		public String toString() {
			String constrainedPattern = String.format("%s", pattern);
			if (constrainedPattern != null) {
				constrainedPattern = String.format("%s and %s", pattern, constraint.toString());
			}
			return String.format("got (%s) then (%s)", constrainedPattern, clause.toString());
		}
	}

	// TODO: Somehow restrict access to the private scope
	public static class ModuleExpr extends Expr {
		public String name;
		public ArrayList<Expr> body;

		public Atom eval(Scope scope) throws Exception {
			ModuleScope moduleScope = new ModuleScope(name, scope);
			// publicScope.searchChildScopes(true); // Add circular dependency
			for (Expr expr : this.body) {
				if (expr instanceof Expr.PublicExpr) {
					moduleScope.setToPrivateEnv(false);
					expr.eval(moduleScope);
				} else {
					moduleScope.setToPrivateEnv(true);
					expr.eval(moduleScope);
				}
			}
			// publicScope.searchChildScopes(false);
			return new Atom.UnitBox(scope.set(this.name, new Atom.Module(name, body, moduleScope)));
		}

		public ModuleExpr(String name, ArrayList<Expr> body, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.name = name;
			this.body = body;
		}

		public String toString() {
			return String.format("mod %s {\n\t%s\n}", name,
					String.join(",\n\t", body.stream().map(Expr::toString).toList()));
		}
	}

	public static class ImportExpr extends Expr {
		ArrayList<String> importList;
		String fileName;

		public Atom eval(Scope scope) throws Exception {
			Path currentPath = Paths.get(scope.getSourceFileDirectory());
			Path filePath1 = Paths.get(fileName);
			Path filePath2 = currentPath.resolve(fileName);
			String source = FileHelper.readFile(filePath2);
			Interpreter i = new Interpreter();
			String p1 = filePath2.getParent().toString();
			String p2 = filePath2.getParent().normalize().toAbsolutePath().toString();
			i.evalAll(source, p2);
			HashMap<String, Atom> importedExports = i.getGlobalScope().getExports();
			for (String importName : importList) {
				if (!importedExports.containsKey(importName)) {
					throw new Exception("File " + fileName + " does not export " + importName + "!");
				}
				// Identifier is exported from the imported file
				scope.set(importName, importedExports.get(importName));
			}
			return new Atom.Unit();
		}

		public ImportExpr(ArrayList<String> importList, String fileName, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.importList = importList;
			this.fileName = fileName;
		}

		public ImportExpr(ArrayList<String> importList, String fileName) {
			this(importList, fileName, -1, -1);
		}

		public String toString() {
			return String.format("imp %s from \"%s\"", String.join(", ", importList), fileName);
		}
	}

	public static class LambdaCall extends Expr {
		Atom identifier;
		ArrayList<Expr> variables;

		public Atom eval(Scope scope) throws Exception {
			Scope callScope;

			Atom.Lambda lambda = ((Atom.Lambda) scope.getByIdent(this.identifier, scope.getID()));
			if (lambda != null) {
				Scope lambdaScope = lambda.getScope();
				callScope = lambdaScope.deriveNew("Lambda call " + identifier.toString());
				return evalLambda(lambda, scope, callScope);
			}
			if (this.identifier instanceof Atom.Ident) {
				String progFuncName = ((Atom.Ident) this.identifier).name;
				ProgramFunction pf = scope.getProgramFunction(progFuncName);
				callScope = scope.deriveNew("Builtin call " + progFuncName);
				if (pf != null) {
					return evalProgFunc(pf, callScope);
				}
			}
			throw new Exception(
					String.format("Undefined function '%s' in %s", this.identifier.toString(), scope.getName()));
		}

		public Atom evalProgFunc(ProgramFunction pf, Scope evaledScope) throws Exception {
			ArrayList<Atom> args = new ArrayList<>();
			for (Expr expr : this.variables) {
				args.add(expr.eval(evaledScope));
			}
			return pf.call(args);
		}

		public Atom evalLambda(Atom.Lambda lambda, Scope lambdaScope, Scope callScope) throws Exception {
			for (Map.Entry<java.lang.Integer, LambdaVariation> lambdaVariation : lambda.variations.entrySet()) {
				int arity = lambdaVariation.getKey();

				if (this.variables.size() == arity) {
					LambdaVariation variation = lambdaVariation.getValue();
					ArrayList<String> argNames = variation.argNames;
					for (int i = 0; i < argNames.size(); i += 1) {
						callScope.set(argNames.get(i), this.variables.get(i).eval(lambdaScope));
					}
					return variation.expr.eval(callScope);
				}
			}
			throw new Exception(String.format("Could not find function variation matching %s/%s.",
					identifier.toString(), this.variables.size()));
		}

		public LambdaCall(Atom identifier, ArrayList<Expr> variables, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.identifier = identifier;
			this.variables = variables;
		}

		public LambdaCall(Atom identifier, int startIndex, int endIndex) {
			this(identifier, new ArrayList<>(), startIndex, endIndex);
		}

		public LambdaCall(Atom identifier, ArrayList<Expr> variables) {
			this(identifier, variables, -1, -1);
		}

		public LambdaCall(Atom identifier) {
			this(identifier, new ArrayList<>());
		}

		public String toString() {
			return String.format("%s(%s)", identifier.toString(), variables.toString());
		}
	}

	public static class AssignExpr extends Expr {
		String lhs;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			return new Atom.UnitBox(scope.set(lhs, rhs.eval(scope)));
		}

		public AssignExpr(String lhs, Expr rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public AssignExpr(String lhs, Expr rhs) {
			super(-1, -1);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public String toString() {
			return String.format("let %s = %s", lhs, rhs.toString());
		}
	}

	public static class VariationExpr extends Expr {
		String lhs;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			Atom variationValue = rhs.eval(scope);
			if (variationValue instanceof Atom.Lambda) {
				if (scope.has(lhs)) {
					// Add the new variation
					Atom lambda = scope.get(lhs, scope.getID());
					if (lambda instanceof Atom.Lambda) {
						LambdaVariation lambdaVariation = ((Atom.Lambda) variationValue).variations.values().iterator()
								.next();
						((Atom.Lambda) lambda).addVariation(lambdaVariation.expr, lambdaVariation.argNames);
					} else {
						throw new Exception("Cannot add variation to any other than functions");
					}
				} else {
					throw new Exception(String.format("Tried to add variation to nonexistent variable %s", lhs));
				}
			} else {
				throw new Exception("Variation value must be of type lambda expression");
			}
			return new Atom.Unit();
		}

		public VariationExpr(String lhs, Expr rhs, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public String toString() {
			return String.format("var %s = %s", lhs, rhs.toString());
		}
	}

	public static class PublicExpr extends Expr {
		Expr expr;

		public Atom eval(Scope scope) throws Exception {
			Atom result = expr.eval(scope);
			if (result instanceof Atom.UnitBox) {
				// Unpack the result
				result = ((Atom.UnitBox) result).getValue();
			}
			if (scope instanceof GlobalScope && !(result instanceof Atom.Unit)) {
				// Supported exported (pub) expressions
				if (expr instanceof Expr.AssignExpr) {
					String name = ((Expr.AssignExpr) expr).lhs;
					((GlobalScope) scope).export(name, result);
				} else if (expr instanceof Expr.ModuleExpr) {
					String name = ((Expr.ModuleExpr) expr).name;
					((GlobalScope) scope).export(name, result);
				} // Else just return the result
			}
			return new Atom.UnitBox(result);
		}

		public PublicExpr(Expr expr, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.expr = expr;
		}

		public PublicExpr(Expr expr) {
			this(expr, -1, -1);
		}

		public String toString() {
			return String.format("pub %s", expr.toString());
		}
	}

	public static void testExpr() throws Exception {
		// all the eval methods are mutually recursive but since it's essentially a tree
		// instead of a potentially cyclic graph it *is* possible to test them all
		// individually

		Scope emptyScope = new GlobalScope();

		AtomicExpr e1 = new AtomicExpr(new Atom.Integer(1));
		assert ((Atom.Integer) e1.eval(emptyScope)).val == 1;

		Scope piScope = new GlobalScope();
		piScope.set("pi", new Atom.Integer(3));
		AtomicExpr e2 = new AtomicExpr(new Atom.Ident("pi"));
		assert ((Atom.Integer) e2.eval(piScope)).val == 3;

		PrefixExpr e3 = new PrefixExpr(PrefixOp.Negate, new AtomicExpr(new Atom.Integer(3)));
		assert ((Atom.Integer) e3.eval(emptyScope)).val == -3;

		BinaryExpr e4 = new BinaryExpr(BinOp.Add, new Atom.Integer(10), new Atom.Integer(20));
		assert ((Atom.Integer) e4.eval(emptyScope)).val == 30;

		IfExpr e5 = new IfExpr(new AtomicExpr(new Atom.Bool(false)), new AtomicExpr(new Atom.Integer(10)),
				new AtomicExpr(new Atom.Integer(20)));
		assert ((Atom.Integer) e5.eval(emptyScope)).val == 20;

		IfExpr e6 = new IfExpr(new AtomicExpr(new Atom.Bool(true)), new AtomicExpr(new Atom.Integer(10)),
				new AtomicExpr(new Atom.Integer(20)));
		assert ((Atom.Integer) e6.eval(emptyScope)).val == 10;

		Scope lambdaScope = new GlobalScope();

		AtomicExpr fib = (AtomicExpr) Parser.parseExpr("fn (n) => if (n < 2) then (1) else (fib(n - 1) + fib(n - 2))");
		AtomicExpr add = (AtomicExpr) Parser.parseExpr("fn (start, end) => start + end");

		lambdaScope.set("fib", fib.val);
		lambdaScope.set("add", add.val);

		LambdaCall e7 = (LambdaCall) Parser.parseExpr("fib(10)");
		LambdaCall e8 = (LambdaCall) Parser.parseExpr("add(5, 10)");
		assert ((Atom.Integer) e7.eval(lambdaScope)).val == 89;
		assert ((Atom.Integer) e8.eval(lambdaScope)).val == 15;

		Scope newScope = new GlobalScope();

		AssignExpr e9 = (AssignExpr) Parser.parseExpr("let x = 15");
		e9.eval(newScope);
		assert ((Atom.Integer) newScope.get("x")).val == 15;
		AssignExpr e10 = (AssignExpr) Parser.parseExpr("let x = x * x");
		e10.eval(newScope);
		assert ((Atom.Integer) newScope.get("x")).val == 15 * 15;
	}
}

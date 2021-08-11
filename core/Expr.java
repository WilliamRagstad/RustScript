package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import core.Atom.Lambda.LambdaVariation;
import core.formatting.EscapeSequence;

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
				var res = scope.get(v.name);
				if (res == null) {
					throw new Exception(String.format("Tried to access nonexistent variable %s", v.name));
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
			Scope clausScope = scope.clone();
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

	public static class LambdaCall extends Expr {
		String name;
		ArrayList<Expr> variables;

		public Atom eval(Scope scope) throws Exception {
			Scope evaledScope = scope.clone();
			// HashMap<String, Atom> evaledVariables = new HashMap<>();
			// evaledVariables.putAll(variables);

			Atom.Lambda lambda = ((Atom.Lambda) scope.get(this.name));
			if (lambda != null)
				return evalLambda(lambda, evaledScope);
			ProgramFunction pf = scope.getProgramFunction(this.name);
			if (pf != null)
				return evalProgFunc(pf, evaledScope);
			throw new Exception(String.format("Undefined function '%s'", this.name));
		}

		public Atom evalProgFunc(ProgramFunction pf, Scope evaledScope) throws Exception {
			ArrayList<Atom> args = new ArrayList<>();
			for (Expr expr : this.variables) {
				args.add(expr.eval(evaledScope));
			}
			return pf.call(args);
		}

		public Atom evalLambda(Atom.Lambda lambda, Scope evaledScope) throws Exception {
			for (Map.Entry<java.lang.Integer, LambdaVariation> lambdaVariation : lambda.variations.entrySet()) {
				int arity = lambdaVariation.getKey();

				if (this.variables.size() == arity) {
					LambdaVariation variation = lambdaVariation.getValue();
					ArrayList<String> argNames = variation.argNames;
					for (int i = 0; i < argNames.size(); i += 1) {
						evaledScope.set(argNames.get(i), this.variables.get(i).eval(evaledScope));
					}
					return variation.expr.eval(evaledScope);
				}
			}
			throw new Exception(
					String.format("Could not find function variation matching %s/%s.", name, this.variables.size()));
		}

		public LambdaCall(String name, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.name = name;
		}

		public LambdaCall(String name) {
			super(-1, -1);
			this.name = name;
		}

		public LambdaCall(String name, ArrayList<Expr> variables, int startIndex, int endIndex) {
			super(startIndex, endIndex);
			this.name = name;
			this.variables = variables;
		}

		public LambdaCall(String name, ArrayList<Expr> variables) {
			super(-1, -1);
			this.name = name;
			this.variables = variables;
		}

		public String toString() {
			return String.format("%s(%s)", name, variables.toString());
		}
	}

	public static class AssignExpr extends Expr {
		String lhs;
		Expr rhs;

		public Atom eval(Scope scope) throws Exception {
			scope.set(lhs, rhs.eval(scope));
			return new Atom.Unit();
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
					Atom lambda = scope.get(lhs);
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

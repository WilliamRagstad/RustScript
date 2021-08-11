package core;

import java.util.ArrayList;
import java.util.HashMap;

import core.formatting.EscapeSequence;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 *
 *         An Atom is any discrete variable, literal or not
 *
 *         <p>
 *         It's worth noting that this is a significant reason why the
 *         interpreter is so slow; each Atom has a whole lot of space and time
 *         overhead because of how Java stores things. Because of this
 *         optimization is not a priority. To get around this, we could use a
 *         bytecode interpreter which would be orders of magnitude faster but a
 *         bit more complex.
 *         </p>
 */
public abstract class Atom {
	public static class Integer extends Atom {
		public int val;

		public Integer(int val) {
			this.val = val;
		}

		public String toString() {
			return String.valueOf(val);
		}
	}

	public static class Float extends Atom {
		public double val;

		public Float(double val) {
			this.val = val;
		}

		public String toString() {
			return String.valueOf(val);
		}
	}

	public static class Bool extends Atom {
		public boolean val;

		public Bool(boolean val) {
			this.val = val;
		}

		public String toString() {
			return String.valueOf(val);
		}
	}

	public static class Char extends Atom {
		public char val;

		public Char(char val) {
			this.val = val;
		}

		public char getCharValue() {
			return this.val;
		}

		public String toString() {
			return "\'" + EscapeSequence.escape(getCharValue()) + "\'";
		}
	}

	public static class List extends Atom {
		public ArrayList<Expr> list;

		public List(ArrayList<Expr> list) {
			this.list = list;
		}

		public boolean isCharArray() {
			for (int i = 0; i < list.size(); i++) {
				Expr e = list.get(i);
				if (!(e instanceof Expr.AtomicExpr && ((Expr.AtomicExpr) e).val instanceof Atom.Char))
					return false;
			}
			return true;
		}

		/**
		 * Convert list of characters to string
		 *
		 * @param escapeCharacters Whether to show escaping of special characters using
		 *                         backslash notation
		 * @return
		 */
		public String getStringValue(boolean escapeCharacters) {
			String result = "";
			for (int i = 0; i < list.size(); i++) {
				char c = ((Atom.Char) ((Expr.AtomicExpr) list.get(i)).val).val;
				if (escapeCharacters) {
					result += EscapeSequence.escape(c);
				} else {
					result += c;
				}
			}
			return result;
		}

		public String toString() {
			if (isCharArray())
				return String.format("\"%s\"", getStringValue(true));
			return list.toString();
		}
	}

	public static class Str extends List {
		private static ArrayList<Expr> split(String val) {
			ArrayList<Expr> list = new ArrayList<Expr>();
			for (int i = 0; i < val.length(); i++) {
				list.add(new Expr.AtomicExpr(new Atom.Char(val.charAt(i))));
			}
			return list;
		}

		public Str(String val) {
			super(split(val));
		}

		public String toString() {
			return super.toString();
		}
	}

	public static class Ident extends Atom {
		public String name;

		public Ident(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	public static class Lambda extends Atom {
		public HashMap<java.lang.Integer, LambdaVariation> variations;

		public Lambda(Expr expr, ArrayList<String> argNames) {
			variations = new HashMap<java.lang.Integer, LambdaVariation>();
			variations.put(argNames.size(), new LambdaVariation(expr, argNames));
		}

		public void addVariation(Expr expr, ArrayList<String> argNames) {
			if (variations.containsKey(argNames.size())) {
				throw new RuntimeException("Lambda already has a variation with arity " + argNames.size());
			}
			variations.put(argNames.size(), new LambdaVariation(expr, argNames));
		}

		public String toString() {
			return String.format("Lambda [\n\t%s\n]",
					String.join(",\n\t", variations.values().stream().map(LambdaVariation::toString).toList()));
		}

		public static class LambdaVariation {
			public Expr expr;
			public ArrayList<String> argNames;

			public LambdaVariation(Expr expr, ArrayList<String> argNames) {
				this.expr = expr;
				this.argNames = argNames;
			}

			public String toString() {
				return String.format("{argNames: %s, expr: %s}", argNames.toString(), expr.toString());
			}
		}
	}

	public static class MatchCaseResult extends Atom {
		private boolean matched;
		private Atom value;

		private MatchCaseResult(boolean matched, Atom value) {
			this.matched = matched;
			this.value = value;
		}

		public static MatchCaseResult matched(Atom value) {
			return new MatchCaseResult(true, value);
		}

		public static MatchCaseResult noMatch() {
			return new MatchCaseResult(false, null);
		}

		public boolean isMatch() {
			return matched;
		}

		public Atom getClauseValue() {
			return value;
		}
	}

	public static class Unit extends Atom {

		public Unit() {
		}

		public String toString() {
			return String.format("()");
		}
	}

	public Atom add(Atom rhs) throws Exception {
		if (this instanceof List && !(this instanceof Str) && ((List) this).isCharArray()) {
			// If a List but classify as Str, convert it
			return new Atom.Str(((List) this).getStringValue(false)).add(rhs);
		}

		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return new Integer(((Integer) this).val + ((Integer) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Integer)) {
			return new Float(((Float) this).val + ((Integer) rhs).val);
		} else if ((this instanceof Integer) && (rhs instanceof Float)) {
			return new Float(((Integer) this).val + ((Float) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Float)) {
			return new Float(((Float) this).val + ((Float) rhs).val);
		} else if (this instanceof Char && rhs instanceof Integer) {
			char c = ((Char) this).val;
			int ci = (int) c;
			int ad = ((Integer) rhs).val;
			char res = (char) (ci + ad);
			return new Char(res);
		} else if (this instanceof Str && rhs instanceof Str) {
			return new Atom.Str(((Str) this).getStringValue(false) + ((Str) rhs).getStringValue(false));
		} else if (this instanceof Str && !(rhs instanceof List)) { // Str is List
			String value = ((Atom.Str) this).getStringValue(false);
			if (rhs instanceof Char) {
				String newstring = value + ((Atom.Char) rhs).val;
				return new Atom.Str(newstring);
			}
			return new Atom.Str(value + rhs.toString());
			// else Badd
		} else if ((this instanceof List) && (rhs instanceof List)) {
			List lArr = (List) this;
			List rArr = (List) rhs;

			List newList = new List(new ArrayList<Expr>());
			newList.list.addAll(lArr.list);
			newList.list.addAll(rArr.list);
			if (newList.isCharArray())
				return new Atom.Str(newList.getStringValue(false));
			return newList;
		}
		throw new Exception("Badd");
	}

	public Atom sub(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return new Integer(((Integer) this).val - ((Integer) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Integer)) {
			return new Float(((Float) this).val - ((Integer) rhs).val);
		} else if ((this instanceof Integer) && (rhs instanceof Float)) {
			return new Float(((Integer) this).val - ((Float) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Float)) {
			return new Float(((Float) this).val - ((Float) rhs).val);
		} else if (this instanceof Char && rhs instanceof Integer) {
			char c = ((Char) this).val;
			int ci = (int) c;
			int rm = ((Integer) rhs).val;
			char res = (char) (ci - rm);
			return new Char(res);
		} else {
			throw new Exception("Bad Sub");
		}
	}

	public Atom mul(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Integer(((Integer) this).val * ((Integer) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Integer)) {
			return new Float(((Float) this).val * ((Integer) rhs).val);
		} else if ((this instanceof Integer) && (rhs instanceof Float)) {
			return new Float(((Integer) this).val * ((Float) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Float)) {
			return new Float(((Float) this).val * ((Float) rhs).val);
		} else {
			throw new Exception("Bad Mul");
		}
	}

	public Atom div(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Integer(((Integer) this).val / ((Integer) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Integer)) {
			return new Float(((Float) this).val / ((Integer) rhs).val);
		} else if ((this instanceof Integer) && (rhs instanceof Float)) {
			return new Float(((Integer) this).val / ((Float) rhs).val);
		} else if ((this instanceof Float) && (rhs instanceof Float)) {
			return new Float(((Float) this).val / ((Float) rhs).val);
		} else {
			throw new Exception("Bad Div");
		}
	}

	public Atom mod(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Integer(((Integer) this).val % ((Integer) rhs).val);
		} else {
			throw new Exception("Bad Mod");
		}
	}

	public Atom lt(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Bool(((Integer) this).val < ((Integer) rhs).val);
		} else if (this instanceof Char && rhs instanceof Char) {
			return (Atom) new Bool(((Char) this).val < ((Char) rhs).val);
		} else {
			throw new Exception("Bad Cmp");
		}
	}

	public Atom gt(Atom rhs) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Bool(((Integer) this).val > ((Integer) rhs).val);
		} else if (this instanceof Char && rhs instanceof Char) {
			return (Atom) new Bool(((Char) this).val > ((Char) rhs).val);
		} else {
			throw new Exception("Bad Cmp");
		}
	}

	public Atom eq(Atom rhs, Scope scope) throws Exception {
		if ((this instanceof Integer) && (rhs instanceof Integer)) {
			return (Atom) new Bool(((Integer) this).val == ((Integer) rhs).val);
		} else if (this instanceof Bool || rhs instanceof Bool) {
			return (Atom) new Bool(this.isTruthy() == rhs.isTruthy());
		} else if (this instanceof Char || rhs instanceof Char) {
			return (Atom) new Bool(((Char) this).val == ((Char) rhs).val);
		} else if (this instanceof List && rhs instanceof List) {
			List lhs = (List) this;
			List other = (List) rhs;
			if (lhs.list.size() != other.list.size())
				return (Atom) new Bool(false);
			for (int i = 0; i < lhs.list.size(); i++) {
				Atom first = lhs.list.get(i).eval(scope);
				Atom second = other.list.get(i).eval(scope);
				if (!first.eq(second, scope).isTruthy())
					return (Atom) new Bool(false);
			}
			return (Atom) new Bool(true);
		} else {
			throw new Exception("Bad Cmp");
		}
	}

	public Atom negate() throws Exception {
		if (this instanceof Integer) {
			Integer v = (Integer) this;
			return (Atom) new Integer(-v.val);
		} else if (this instanceof Bool) {
			Bool b = (Bool) this;
			return (Atom) new Bool(!b.val);
		} else {
			throw new Exception("Bad Negate");
		}
	}

	public Atom head(Scope scope) throws Exception {
		if (this instanceof List) {
			List ls = (List) this;
			return (Atom) ls.list.get(0).eval(scope);
		} else {
			throw new Exception("Bad Head");
		}
	}

	public Atom tail() throws Exception {
		if (this instanceof List) {
			List ls = (List) this;
			ArrayList<Expr> nls = new ArrayList<>(ls.list.subList(1, ls.list.size()));
			return (Atom) new List(nls);
		} else {
			throw new Exception("Bad Tail");
		}
	}

	public boolean isTruthy() throws Exception {
		if (this instanceof Bool) {
			Bool v = (Bool) this;
			return v.val;
		} else if (this instanceof List) {
			List ls = (List) this;
			return !ls.list.isEmpty();
		} else {
			throw new Exception(String.format("Can't coerce %s to a boolean", this.toString()));
		}
	}

	public Atom and(Atom rhs) throws Exception {
		if (this instanceof Bool && rhs instanceof Bool) {
			Bool lhs = (Bool) this;
			Bool other = (Bool) rhs;
			return new Bool(lhs.val && other.val);
		} else {
			throw new Exception(String.format("Can't coerce %s to a boolean", this.toString()));
		}
	}

	public Atom or(Atom rhs) throws Exception {
		if (this instanceof Bool && rhs instanceof Bool) {
			Bool lhs = (Bool) this;
			Bool other = (Bool) rhs;
			return new Bool(lhs.val || other.val);
		} else {
			throw new Exception(String.format("Can't coerce %s to a boolean", this.toString()));
		}
	}
}

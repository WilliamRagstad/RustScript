package core;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 * 
 *          An Atom is any discrete variable, literal or not
 *
 *          <p>
 *          It's worth noting that this is a significant reason why the
 *          interpreter is so slow; each Atom has a whole lot of space and time
 *          overhead because of how Java stores things. Because of this
 *          optimization is not a priority. To get around this, we could use a
 *          bytecode interpreter which would be orders of magnitude faster but a
 *          bit more complex.
 *          </p>
 */
public abstract class Atom {
    public static class Val extends Atom {
        public int val;

        public Val(int val) {
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
            return '\'' + String.valueOf(val) + '\'';
        }
    }

    public static class List extends Atom {
        public ArrayList<Expr> list;

        public List(ArrayList<Expr> list) {
            this.list = list;
        }
        
        public boolean isCharArray() {
        	for(int i = 0; i < list.size(); i++) {
        		Expr e = list.get(i);
        		if (!(e instanceof Expr.AtomicExpr && ((Expr.AtomicExpr)e).val instanceof Atom.Char)) return false;
        	}
        	return true;
        }
        public String getStringValue() {
            String result = "";
            for(int i = 0; i < list.size(); i++) {
        		result += ((Atom.Char)((Expr.AtomicExpr)list.get(i)).val).val;
        	}
        	return result;
        }
        public String toString() {
        	if (isCharArray()) return String.format("\"%s\"", getStringValue());
            return list.toString();
        }
    }
    
    public static class Str extends List {
    	private static ArrayList<Expr> split(String val) {
    		ArrayList<Expr> list = new ArrayList<Expr>();
    		for(int i = 0; i < val.length(); i++) {
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
            return String.format("\"%s\"", name);
        }
    }

    public static class Lambda extends Atom {
        public Expr expr;
        public ArrayList<String> argNames;

        public Lambda(Expr expr, ArrayList<String> argNames) {
            this.expr = expr;
            this.argNames = argNames;
        }

        public String toString() {
            return String.format("Lambda {expr: %s, argNames: %s}", expr.toString(), argNames.toString());
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
        if (this instanceof List && !(this instanceof Str) && ((List)this).isCharArray()) {
            // If a List but classify as Str, convert it
            return new Atom.Str(((List)this).getStringValue()).add(rhs);
        }

        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Val(((Val) this).val + ((Val) rhs).val);
        } else if (this instanceof Str && rhs instanceof Str) {
            return new Atom.Str(((Str)this).getStringValue() + ((Str)rhs).getStringValue());
        } else if (this instanceof Str && !(rhs instanceof List)) { // Str is List
            String value = ((Atom.Str)this).getStringValue();
            if (rhs instanceof Val || rhs instanceof Bool)  return new Atom.Str(value + rhs.toString());
            else if (rhs instanceof Char)                   return new Atom.Str(value + ((Atom.Char)rhs).val);
            // else Badd
        } else if ((this instanceof List) && (rhs instanceof List)) {
            List lArr = (List) this;
            List rArr = (List) rhs;

            List newList = new List(new ArrayList<Expr>());
            newList.list.addAll(lArr.list);
            newList.list.addAll(rArr.list);
            if (newList.isCharArray()) return new Atom.Str(newList.getStringValue());
            return (Atom) newList;
        }
        throw new Exception("Badd");
    }

    public Atom sub(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Val(((Val) this).val - ((Val) rhs).val);
        } else {
            throw new Exception("Bad Sub");
        }
    }

    public Atom mul(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Val(((Val) this).val * ((Val) rhs).val);
        } else {
            throw new Exception("Bad Mul");
        }
    }

    public Atom div(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Val(((Val) this).val / ((Val) rhs).val);
        } else {
            throw new Exception("Bad Div");
        }
    }

    public Atom mod(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Val(((Val) this).val % ((Val) rhs).val);
        } else {
            throw new Exception("Bad Mod");
        }
    }

    public Atom lt(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Bool(((Val) this).val < ((Val) rhs).val);
        } else if (this instanceof Char && rhs instanceof Char) {
            return (Atom) new Bool(((Char) this).val < ((Char) rhs).val);
        } else {
            throw new Exception("Bad Cmp");
        }
    }

    public Atom gt(Atom rhs) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom) new Bool(((Val) this).val > ((Val) rhs).val);
        } else if (this instanceof Char && rhs instanceof Char) {
            return (Atom) new Bool(((Char) this).val > ((Char) rhs).val);
        } else {
            throw new Exception("Bad Cmp");
        }
    }

    public Atom eq(Atom rhs, HashMap<String, Atom> variables, HashMap<String, ProgramFunction> program) throws Exception {
        if ((this instanceof Val) && (rhs instanceof Val)) {
            return (Atom)new Bool(((Val) this).val == ((Val) rhs).val);
        }
        else if (this instanceof Bool || rhs instanceof Bool) {
            return (Atom)new Bool(this.isTruthy() == rhs.isTruthy());
        }
        else if (this instanceof Char || rhs instanceof Char) {
            return (Atom)new Bool(((Char)this).val == ((Char)rhs).val);
        }
        else if (this instanceof List && rhs instanceof List) {
            List lhs   = (List)this;
            List other = (List)rhs;
            if (lhs.list.size() != other.list.size()) return (Atom)new Bool(false);
            for (int i = 0; i < lhs.list.size(); i++) {
                Atom first = lhs.list.get(i).eval(variables, program);
                Atom second = other.list.get(i).eval(variables, program);
                if (!first.eq(second, variables, program).isTruthy()) return (Atom)new Bool(false);
            }
            return (Atom)new Bool(true);
        }
        else {
            throw new Exception("Bad Cmp");
        }
    }

    public Atom negate() throws Exception {
    	if (this instanceof Val) {
            Val v = (Val) this;
            return (Atom) new Val(-v.val);
        }
    	else if (this instanceof Bool) {
            Bool b = (Bool) this;
            return (Atom) new Bool(!b.val);
        }
    	else {
            throw new Exception("Bad Negate");
        }
    }

    public Atom head(HashMap<String, Atom> variables, HashMap<String, ProgramFunction> program) throws Exception {
        if (this instanceof List) {
            List ls = (List) this;
            return (Atom) ls.list.get(0).eval(variables, program);
        } else {
            throw new Exception("Bad Head");
        }
    }

    public Atom tail(HashMap<String, Atom> variables) throws Exception {
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

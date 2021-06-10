import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 * 
 *          An expression, the AST of this language.
 *
 *          <p>
 *          Because this is a functional expression based language, there are no
 *          statements, only expressions. In other words, everything returns
 *          something, even if it's just the unit type.
 *          </p>
 */
public abstract class Expr {
    public int startIndex, endIndex;
    abstract Atom eval(HashMap<String, Atom> variables) throws Exception;

    public Expr(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public static class AtomicExpr extends Expr {
        Atom val;

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            if (val instanceof Atom.Ident) {
                Atom.Ident v = (Atom.Ident) val;
                var res = variables.get(v.name);
                if (res == null) {
                    throw new Exception(String.format("Tried to access nonexistent variable %s", v.name));
                }
                return res;
            } else if (val instanceof Atom.List) {
                Atom.List ls = (Atom.List) val;
                ArrayList<Expr> nls = new ArrayList<>();
                for (Expr expr : ls.list) {
                    nls.add(new AtomicExpr(expr.eval(variables)));
                }
                return new Atom.List(nls);
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
            return String.valueOf(val);
        }
    }

    public static class PrefixExpr extends Expr {
        PrefixOp op;
        Expr rhs;

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            return switch (op) {
                case Negate -> rhs.eval(variables).negate();
                case Head -> rhs.eval(variables).head(variables);
                case Tail -> rhs.eval(variables).tail(variables);
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

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            return switch (op) {
                case Add -> lhs.eval(variables).add(rhs.eval(variables));
                case Sub -> lhs.eval(variables).sub(rhs.eval(variables));
                case Mul -> lhs.eval(variables).mul(rhs.eval(variables));
                case Div -> lhs.eval(variables).div(rhs.eval(variables));
                case Mod -> lhs.eval(variables).mod(rhs.eval(variables));
                case LT -> lhs.eval(variables).lt(rhs.eval(variables));
                case GT -> lhs.eval(variables).gt(rhs.eval(variables));
                case EQ -> lhs.eval(variables).eq(rhs.eval(variables));
                case And -> lhs.eval(variables).and(rhs.eval(variables));
                case Or -> lhs.eval(variables).or(rhs.eval(variables));
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

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            Atom condVal = cond.eval(variables);
            if (condVal.isTruthy()) {
                return lhs.eval(variables);
            } else {
                return rhs.eval(variables);
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

    public static class LambdaCall extends Expr {
        String name;
        ArrayList<Expr> variables;

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            HashMap<String, Atom> evaledVariables = new HashMap<>();
            evaledVariables.putAll(variables);

            Atom.Lambda lambda = ((Atom.Lambda) evaledVariables.get(this.name));
            if (lambda == null) {
                throw new Exception(String.format("Undefined lambda '%s'", this.name));
            }

            ArrayList<String> argNames = lambda.argNames;

            if (this.variables.size() != lambda.argNames.size()) {
                throw new Exception(String.format("Expected %d arguments to call of lambda %s, got %d",
                        lambda.argNames.size(), name, this.variables.size()));
            }

            for (int i = 0; i < argNames.size(); i += 1) {
                evaledVariables.put(argNames.get(i), this.variables.get(i).eval(variables));
            }

            Expr expr = lambda.expr;
            return expr.eval(evaledVariables);
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

        Atom eval(HashMap<String, Atom> variables) throws Exception {
            variables.put(lhs, rhs.eval(variables));
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

    public static void testExpr() throws Exception {
        // all the eval methods are mutually recursive but since it's essentially a tree
        // instead of a potentially cyclic graph it *is* possible to test them all
        // individually

        HashMap<String, Atom> emptyScope = new HashMap<>();

        AtomicExpr e1 = new AtomicExpr(new Atom.Val(1));
        assert ((Atom.Val) e1.eval(emptyScope)).val == 1;

        HashMap<String, Atom> piScope = new HashMap<>();
        piScope.put("pi", new Atom.Val(3));
        AtomicExpr e2 = new AtomicExpr(new Atom.Ident("pi"));
        assert ((Atom.Val) e2.eval(piScope)).val == 3;

        PrefixExpr e3 = new PrefixExpr(PrefixOp.Negate, new AtomicExpr(new Atom.Val(3)));
        assert ((Atom.Val) e3.eval(emptyScope)).val == -3;

        BinaryExpr e4 = new BinaryExpr(BinOp.Add, new Atom.Val(10), new Atom.Val(20));
        assert ((Atom.Val) e4.eval(emptyScope)).val == 30;

        IfExpr e5 = new IfExpr(new AtomicExpr(new Atom.Bool(false)), new AtomicExpr(new Atom.Val(10)),
                new AtomicExpr(new Atom.Val(20)));
        assert ((Atom.Val) e5.eval(emptyScope)).val == 20;

        IfExpr e6 = new IfExpr(new AtomicExpr(new Atom.Bool(true)), new AtomicExpr(new Atom.Val(10)),
                new AtomicExpr(new Atom.Val(20)));
        assert ((Atom.Val) e6.eval(emptyScope)).val == 10;

        HashMap<String, Atom> lambdaScope = new HashMap<>();

        AtomicExpr fib = (AtomicExpr) Parser.parseExpr("fn (n) => if (n < 2) then (1) else (fib(n - 1) + fib(n - 2))");
        AtomicExpr add = (AtomicExpr) Parser.parseExpr("fn (start, end) => start + end");

        lambdaScope.put("fib", fib.val);
        lambdaScope.put("add", add.val);

        LambdaCall e7 = (LambdaCall) Parser.parseExpr("fib(10)");
        LambdaCall e8 = (LambdaCall) Parser.parseExpr("add(5, 10)");
        assert ((Atom.Val) e7.eval(lambdaScope)).val == 89;
        assert ((Atom.Val) e8.eval(lambdaScope)).val == 15;

        HashMap<String, Atom> newScope = new HashMap<>();

        AssignExpr e9 = (AssignExpr) Parser.parseExpr("let x = 15");
        e9.eval(newScope);
        assert ((Atom.Val) newScope.get("x")).val == 15;
        AssignExpr e10 = (AssignExpr) Parser.parseExpr("let x = x * x");
        e10.eval(newScope);
        assert ((Atom.Val) newScope.get("x")).val == 15 * 15;
    }
}
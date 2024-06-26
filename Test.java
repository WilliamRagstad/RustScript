import java.util.ArrayList;
import core.*;

public class Test {
    public static void main(String[] args) throws Exception {
        Tokenizer.testTokenizer();
        Parser.testParser();
        Expr.testExpr();

        // Som be full stack tests
        //
        // I use toString as a hash function to test for equality

        Interpreter i = new Interpreter();

        Atom val1 = i.eval("5 + 12 * 3 - 2");
        assert val1 instanceof Atom.Integer;
        assert ((Atom.Integer) val1).val == 39;

        Atom val2 = i.eval("(5 + -12) * (3 - -2)");
        assert val2 instanceof Atom.Integer;
        assert ((Atom.Integer) val2).val == -35;

        Atom val3 = i.eval("let x = 5");
        assert val3 instanceof Atom.Unit;

        Atom val4 = i.eval("x");
        assert val4 instanceof Atom.Integer;
        assert ((Atom.Integer) val4).val == 5;

        i.eval("let fib = fn (n) => if (n < 2) then (1) else (fib(n - 1) + fib(n - 2))");
        Atom val5 = i.eval("fib(10)");
        assert val5 instanceof Atom.Integer;
        assert ((Atom.Integer) val5).val == 89;

        Atom val6 = i.eval("range(5, 10)");
        assert val6 instanceof Atom.List;
        ArrayList<Expr> list1 = new ArrayList<>();
        list1.add(new Expr.AtomicExpr(new Atom.Integer(5)));
        list1.add(new Expr.AtomicExpr(new Atom.Integer(6)));
        list1.add(new Expr.AtomicExpr(new Atom.Integer(7)));
        list1.add(new Expr.AtomicExpr(new Atom.Integer(8)));
        list1.add(new Expr.AtomicExpr(new Atom.Integer(9)));
        assert ((Atom.List) val6).list.toString().equals(list1.toString());

        Atom val7 = i.eval("range(0, 20)");
        Atom val8 = i.eval("[0..20]");
        assert ((Atom.List) val7).list.toString().equals(((Atom.List) val8).list.toString());

        Atom val9 = i.eval("fmap(fn (n) => n * 2, [0..3]))");
        ArrayList<Expr> list2 = new ArrayList<>();
        list2.add(new Expr.AtomicExpr(new Atom.Integer(0)));
        list2.add(new Expr.AtomicExpr(new Atom.Integer(2)));
        list2.add(new Expr.AtomicExpr(new Atom.Integer(4)));
        assert ((Atom.List) val9).list.toString().equals(list2.toString());

        Atom val10 = i.eval("filter(fn (n) => n % 3 == 0, [0..10])");
        ArrayList<Expr> list3 = new ArrayList<>();
        list3.add(new Expr.AtomicExpr(new Atom.Integer(0)));
        list3.add(new Expr.AtomicExpr(new Atom.Integer(3)));
        list3.add(new Expr.AtomicExpr(new Atom.Integer(6)));
        list3.add(new Expr.AtomicExpr(new Atom.Integer(9)));
        assert ((Atom.List) val10).list.toString().equals(list3.toString());

        Atom val11 = i.eval("fold(fn (acc, n) => acc + n, 0, [1..1000])");
        Atom val12 = i.eval("sum(range(1, 1000))");

        assert ((Atom.Integer) val11).val == 499500;
        assert ((Atom.Integer) val11).val == ((Atom.Integer) val12).val;

        i.eval("let fib_step = fn (ls, i) => [^$ls, ^ls + ^$ls]");
        i.eval("let efficient_fib = fn (n) => ^$fold(fib_step, [1, 1], [0..n])");
        Atom val13 = i.eval("efficient_fib(30)");

        assert ((Atom.Integer) val13).val == 2178309;

        System.out.println("All tests passed!");
    }
}

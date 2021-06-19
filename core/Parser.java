package core;
import java.util.ArrayList;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 * 
 *          A Parser takes a String, tokenizes it using a Tokenizer, and then
 *          converts the flat list of tokens into a meaningful Expr AST which
 *          can be evaluated.
 *
 *          <p>
 *          https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html
 *          <br>
 *          I read this article every time I write a parser.
 *          </p>
 *
 */
public class Parser {
    int position;
    ArrayList<Token> tokens;

    private Parser(ArrayList<Token> tokens) {
        this.position = 0;
        this.tokens = tokens;
    }

    private boolean isFinished() {
        return this.position >= this.tokens.size();
    }

    private Token peek() {
        return isFinished() ? Token.EOF(position) : tokens.get(position);
    }

    private Token eat() {
        if (!isFinished()) {
            Token ret = tokens.get(position);
            position += 1;
            return ret;
        } else {
            return Token.EOF(position);
        }
    }

    private boolean expect(TokenTy expected) {
        if (isFinished())
            return false;

        Token c = tokens.get(position);
        if (c.ty == expected) {
            position += 1;
            return true;
        } else {
            return false;
        }
    }

    private void assertNext(TokenTy expected) throws Exception {
        var nx = eat();
        if (nx.ty != expected) {
            throw new Exception(String.format("Expected %s, got %s", expected.toString(), nx.toString()));
        }
    }

    private Expr parseIfExpr(Token nx) throws Exception {
        Expr cond = exprBP(0);
        assertNext(TokenTy.Then);
        Expr lhs = exprBP(0);
        assertNext(TokenTy.Else);
        Expr rhs = exprBP(0);
        return new Expr.IfExpr(cond, lhs, rhs, nx.index, rhs.endIndex);
    }
    private Expr parseIfExpr() throws Exception {
        return parseIfExpr(Token.EOF(-1));
    }

    private Expr parseList(Token nx) throws Exception {
        if (peek().ty != TokenTy.RBracket) {
            Expr first = exprBP(0);

            if (peek().ty == TokenTy.For) {
                // list comprehension
                assertNext(TokenTy.For);

                Token ident = eat();
                if (ident.ty != TokenTy.Ident) {
                    throw new Exception("Invalid list comp, expected an identifier after 'for'.");
                }

                String name = ident.lexeme;

                assertNext(TokenTy.In);

                Expr list = exprBP(0);

                ArrayList<String> argNames = new ArrayList<>();
                argNames.add(name);
                Atom mapLambda = new Atom.Lambda(first, argNames);

                ArrayList<Expr> args = new ArrayList<>(2);
                args.add(new Expr.AtomicExpr(mapLambda));
                args.add(list);

                Expr fmap = new Expr.LambdaCall("fmap", args);

                if (expect(TokenTy.If)) {
                    Expr cond = exprBP(0);
                    Atom filterLambda = new Atom.Lambda(cond, argNames);

                    ArrayList<Expr> filterArgs = new ArrayList<>(2);
                    filterArgs.add(new Expr.AtomicExpr(filterLambda));
                    filterArgs.add(fmap);

                    assertNext(TokenTy.RBracket);
                    return new Expr.LambdaCall("filter", filterArgs);
                } else {
                    assertNext(TokenTy.RBracket);
                    return fmap;
                }
            } else if (peek().ty == TokenTy.DotDot) {
                // range literal
                assertNext(TokenTy.DotDot);

                Expr end = exprBP(0);

                ArrayList<Expr> args = new ArrayList<>(2);
                args.add(first);
                args.add(end);

                assertNext(TokenTy.RBracket);
                return new Expr.LambdaCall("range", args);
            } else {
                // array literal
                ArrayList<Expr> out = new ArrayList<>();
                out.add(first);

                if (peek().ty != TokenTy.RBracket) {
                    while (expect(TokenTy.Comma)) {
                        out.add(exprBP(0));
                    }
                }

                assertNext(TokenTy.RBracket);
                return new Expr.AtomicExpr(new Atom.List(out));
            }
        } else {
            Token t = peek();
            assertNext(TokenTy.RBracket);
            return new Expr.AtomicExpr(new Atom.List(new ArrayList<>()), nx.index, t.index + t.length);
        }
    }
    private Expr parseList() throws Exception {
        return parseList(Token.EOF(-1));
    }

    private ArrayList<Expr> parseCallArgs() throws Exception {
        assertNext(TokenTy.LParen);
        ArrayList<Expr> out = new ArrayList<>();

        if (peek().ty != TokenTy.RParen) {
            do {
                out.add(exprBP(0));
            } while (expect(TokenTy.Comma));
        }

        assertNext(TokenTy.RParen);
        return out;
    }

    private Expr parseLetExpr(Token nx) throws Exception {
        Token ident = eat();
        if (ident.ty != TokenTy.Ident) {
            throw new Exception("Invalid let expression");
        }

        assertNext(TokenTy.Assign);

        Expr rhs = exprBP(0);

        return new Expr.AssignExpr(ident.lexeme, rhs, nx.index, rhs.endIndex);
    }
    private Expr parseLetExpr() throws Exception {
        return parseLetExpr(Token.EOF(-1));
    }

    private Expr parseLambdaExpr(Token next) throws Exception {
        assertNext(TokenTy.LParen);

        ArrayList<String> argNames = new ArrayList<>();

        if (peek().ty != TokenTy.RParen) {
            do {
                Token nx = eat();
                if (nx.ty != TokenTy.Ident) {
                    throw new Exception(String.format("Unexpected %s", nx.toString()));
                }
                argNames.add(nx.lexeme);
            } while (expect(TokenTy.Comma));
        }
        assertNext(TokenTy.RParen);

        assertNext(TokenTy.Arrow);

        Expr expr = exprBP(0);

        return new Expr.AtomicExpr(new Atom.Lambda(expr, argNames), next.index, expr.endIndex);
    }
    private Expr parseLambdaExpr() throws Exception {
        return parseLambdaExpr(Token.EOF(-1));
    }

    private Expr exprBP(int minBP) throws Exception {
        return this.exprBP(minBP, false);
    }
    private Expr exprBP(int minBP, boolean allowEOF) throws Exception {
        Token nx = eat();
        int s = nx.index;
        Expr lhs = switch (nx.ty) {
            case True -> new Expr.AtomicExpr(new Atom.Bool(true), s, s+nx.lexeme.length());
            case False -> new Expr.AtomicExpr(new Atom.Bool(false), s, s+nx.lexeme.length());
            case Number -> new Expr.AtomicExpr(new Atom.Val(Integer.parseInt(nx.lexeme)), s, s+nx.lexeme.length());
            case Ident -> {
                if (peek().ty == TokenTy.LParen) {
                    ArrayList<Expr> vars = parseCallArgs();
                    if (vars.size() > 0) yield new Expr.LambdaCall(nx.lexeme, vars, vars.get(0).startIndex, vars.get(vars.size() - 1).endIndex);
                    yield new Expr.LambdaCall(nx.lexeme, vars);
                } else {
                    yield new Expr.AtomicExpr(new Atom.Ident(nx.lexeme), s, s+nx.lexeme.length());
                }
            }
            case Character -> new Expr.AtomicExpr(new Atom.Char(nx.lexeme.charAt(0)), s, s+nx.lexeme.length());
            case String -> new Expr.AtomicExpr(new Atom.Str(nx.lexeme), s, s+nx.lexeme.length());
            case Let -> parseLetExpr(nx);
            case Fn -> parseLambdaExpr(nx);
            case If -> parseIfExpr(nx);
            case LBracket -> parseList(nx);
            case LParen -> {
                Expr temp = exprBP(0);
                expect(TokenTy.RParen); 
                yield temp;
            }
            case Sub, Caret, Dollar -> {
                PrefixOp op = switch (nx.ty) {
                    case Sub -> PrefixOp.Negate;
                    case Caret -> PrefixOp.Head;
                    case Dollar -> PrefixOp.Tail;
                    default -> throw new Exception("Unreachable hopefully");
                };

                PrefixBindingPower bp = new PrefixBindingPower(op);
                Expr rhs = exprBP(bp.right);
                yield new Expr.PrefixExpr(op, rhs, s, rhs.endIndex);
            }
            default -> {
                if (allowEOF && nx.ty == TokenTy.EOF) yield new Expr.AtomicExpr(new Atom.Unit(), s, s+1);
                else throw new Exception(String.format("Expected an expression, found: %s", nx.toString()));
            }
        };

        for (;;) {
            Token opToken = peek();
            BinOp op = switch (opToken.ty) {
                case Add -> BinOp.Add;
                case Sub -> BinOp.Sub;
                case Mul -> BinOp.Mul;
                case Div -> BinOp.Div;
                case Mod -> BinOp.Mod;
                case LT -> BinOp.LT;
                case GT -> BinOp.GT;
                case EQ -> BinOp.EQ;
                case NEQ -> BinOp.NEQ;
                case And -> BinOp.And;
                case Or -> BinOp.Or;
                default -> null;
            };

            if (op == null)
                break;

            //

            BindingPower bp = new BindingPower(op);
            if (bp.left < minBP) {
                break;
            }

            eat();

            Expr rhs = exprBP(bp.right);
            lhs = new Expr.BinaryExpr(op, lhs, rhs, lhs.startIndex, rhs.endIndex);
        }

        return lhs;
    }

    public static Expr parseExpr(String input) throws Exception {
        ArrayList<Token> tokens = Tokenizer.tokenize(input);
        Parser p = new Parser(tokens);

        return p.exprBP(0, true);
    }

    public static void testParser() throws Exception {
        // Testing parser methods individually isn't actually possible since they're
        // mutually recursive.
        //
        // the expression parsers individually expect to come after the first token has
        // been parsed, which is why they're each missing a token.

        {
            // tests parseIfExpr
            ArrayList<Token> tokens = Tokenizer.tokenize("(1) then (2) else (5)");
            Parser p = new Parser(tokens);
            Expr.IfExpr expr = (Expr.IfExpr) p.parseIfExpr();
            assert expr.toString().equals("if (1) then (2) else (5)");
        }

        {
            // tests parseList
            ArrayList<Token> tokens = Tokenizer.tokenize("1, 3, 2, 4]");
            Parser p = new Parser(tokens);
            Expr.AtomicExpr expr = (Expr.AtomicExpr) p.parseList();
            assert expr.toString().equals("[1, 3, 2, 4]");

            ArrayList<Token> tokensRange = Tokenizer.tokenize("0..10]");
            Parser parserRange = new Parser(tokensRange);
            Expr.LambdaCall exprRange = (Expr.LambdaCall) parserRange.parseList();
            assert exprRange.toString().equals("range([0, 10])");

            ArrayList<Token> tokensComp = Tokenizer.tokenize("x * 2 for x in [2..5]]");
            Parser parserComp = new Parser(tokensComp);
            Expr.LambdaCall exprComp = (Expr.LambdaCall) parserComp.parseList();
            assert exprComp.toString().equals("fmap([Lambda {expr: Mul, (\"x\", 2), argNames: [x]}, range([2, 5])])");
        }

        {
            // tests parseCallArgs
            ArrayList<Token> tokens = Tokenizer.tokenize("(2, 4, 6, 8, fib)");
            Parser p = new Parser(tokens);
            ArrayList<Expr> out = p.parseCallArgs();
            assert out.toString().equals("[2, 4, 6, 8, \"fib\"]");
        }

        {
            // tests parseLetExpr
            ArrayList<Token> tokens = Tokenizer.tokenize("x = 5");
            Parser p = new Parser(tokens);
            Expr.AssignExpr expr = (Expr.AssignExpr) p.parseLetExpr();
            assert expr.toString().equals("let x = 5");
        }

        {
            // tests arbitrary arithmetic expr with order of operations
            Expr expr = parseExpr("x + 3 * 5 - 2 / 4");
            assert expr.toString().equals("Sub, (Add, (\"x\", Mul, (3, 5)), Div, (2, 4))");
        }
    }
}

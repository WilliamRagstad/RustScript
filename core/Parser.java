package core;

import java.lang.reflect.Array;
import java.util.ArrayList;

import core.Atom.IdentList;
import core.Expr.MatchCaseExpr;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>, William Rågstad
 *         <william.ragstad@gmail.com>
 *
 *         A Parser takes a String, tokenizes it using a Tokenizer, and then
 *         converts the flat list of tokens into a meaningful Expr AST which can
 *         be evaluated.
 *
 *         <p>
 *         https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html
 *         <br>
 *         I read this article every time I write a parser.
 *         </p>
 *
 */
public class Parser {
	int position, line, column;
	ArrayList<Token> tokens;

	private Parser(ArrayList<Token> tokens) {
		this.position = 0;
		this.tokens = tokens;
	}

	private boolean isFinished() {
		return this.position >= this.tokens.size();
	}

	private Token peek() {
		return peek(true);
	}

	private Token peek(boolean skipWhiteSpace) {
		return peek(skipWhiteSpace, 0);
	}

	private Token peek(boolean skipWhiteSpace, int offset) {
		if (isFinished() || position + offset >= tokens.size())
			return Token.EOF(position, line, column);
		Token ret = tokens.get(position + offset);
		if (skipWhiteSpace && ret.ty == TokenTy.NL)
			return peek(true, offset + 1);
		return ret;
	}

	private Token eat() {
		return eat(true);
	}

	private Token eat(boolean skipWhiteSpace) {
		if (!isFinished()) {
			Token ret = tokens.get(position);
			position += 1;
			line = ret.line;
			column = ret.column;
			if (skipWhiteSpace && ret.ty == TokenTy.NL)
				return eat(true);
			else
				return ret;
		} else {
			return Token.EOF(position, line, column);
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

	private String error(Token at, String... messages) {
		return error(at, String.join("\n\t", messages));
	}

	private String error(Token at, String message) {
		return "Error at line " + at.line + " column " + at.column + ":\n\t" + message;
	}

	private String error(Token start, Token end, String message) {
		return "Error from line " + start.line + " column " + start.column + " to line " + end.line + " column "
				+ end.column + ":\n\t" + message;
	}

	private void assertNext(TokenTy expected) throws Exception {
		var nx = eat();
		if (nx.ty != expected) {
			throw new Exception(error(nx, String.format("Expected %s, got %s", expected.toString(), nx.toString())));
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
		return parseIfExpr(Token.EOF(position, line, column));
	}

	private Expr parseModuleExpr(Token nx) throws Exception {
		Expr name = exprBP(0);
		if (!(name instanceof Expr.AtomicExpr && ((Expr.AtomicExpr) name).val instanceof Atom.Ident)) {
			throw new Exception(error(nx, "Expected module name, got " + name.toString()));
		}
		nx = peek();
		if (nx.ty != TokenTy.LCurlyBracket) {
			throw new Exception(error(nx, "Expected module body"));
		}
		Expr.BlockExpr block = (Expr.BlockExpr) parseBlock(nx);
		return new Expr.ModuleExpr(name.toString(), block.getExprs(), nx.index, block.endIndex);
	}

	private Expr parseImportExpr(Token nx) throws Exception {
		if (peek().ty != TokenTy.Ident) {
			throw new Exception(error(peek(), "Expected import list, got " + peek().toString()));
		}
		ArrayList<Expr> importListExpr = exprBPs(0, true, TokenTy.ImportFrom);
		ArrayList<String> importList = new ArrayList<>();
		for (Expr e : importListExpr) {
			if (!(e instanceof Expr.AtomicExpr && ((Expr.AtomicExpr) e).val instanceof Atom.Ident)) {
				throw new Exception(error(nx, "Expected import identifier name, got " + e.toString()));
			}
			importList.add(((Atom.Ident) ((Expr.AtomicExpr) e).val).name);
		}
		assertNext(TokenTy.ImportFrom);
		Expr fileNameExpr = exprBP(0);
		if (!(fileNameExpr instanceof Expr.AtomicExpr && ((Expr.AtomicExpr) fileNameExpr).val instanceof Atom.Str)) {
			throw new Exception(error(nx, "Expected import string filepath, got " + fileNameExpr.toString()));
		}
		String fileName = ((Atom.Str) ((Expr.AtomicExpr) fileNameExpr).val).getStringValue(false);
		return new Expr.ImportExpr(importList, fileName, nx.index, fileNameExpr.endIndex);
	}

	private Expr parseMatchExpr(Token nx) throws Exception {
		Expr value = exprBP(0);
		ArrayList<MatchCaseExpr> cases = new ArrayList<>();
		while (!isFinished() && peek().ty == TokenTy.MatchCase) {
			eat(); // Eat the Got token
			Token patternToken = peek();
			Expr patternExpr = exprBP(0);
			if (!(patternExpr instanceof Expr.AtomicExpr
					&& ((Expr.AtomicExpr) patternExpr).val instanceof Atom.Ident)) {
				throw new Exception(error(patternToken,
						String.format("Pattern '%s' must be a single identifier.", patternExpr.toString()),
						"If you want to mimic pattern matching, use a constraint by adding 'and <condition>' after wards."));
			}
			Expr constraint = null;
			if (peek().ty == TokenTy.MatchCaseCond) {
				// Parse constraint
				eat(); // Eat the GotAnd token
				constraint = exprBP(0);
			}
			assertNext(TokenTy.Then);
			Expr clause = exprBP(0);

			cases.add(new MatchCaseExpr(value, patternToken.lexeme, constraint, clause, nx.index, clause.endIndex));
		}
		if (cases.isEmpty()) {
			throw new Exception(error(nx, "Match expression must have at least one case"));
		}
		return new Expr.MatchExpr(value, cases, nx.index, value.endIndex);
	}

	private Expr parseList(Token nx) throws Exception {
		if (peek().ty != TokenTy.RBracket) {
			Expr first = exprBP(0);

			if (peek().ty == TokenTy.For) {
				// list comprehension
				assertNext(TokenTy.For);

				Token ident = eat();
				if (ident.ty != TokenTy.Ident) {
					throw new Exception(error(ident, "Invalid list comp, expected an identifier after 'for'."));
				}

				String name = ident.lexeme;

				assertNext(TokenTy.In);

				Expr list = exprBP(0);

				ArrayList<String> argNames = new ArrayList<>();
				argNames.add(name);
				Atom mapLambda = new Atom.Lambda(name, first, argNames);

				ArrayList<Expr> args = new ArrayList<>(2);
				args.add(new Expr.AtomicExpr(mapLambda));
				args.add(list);

				Expr fmap = new Expr.LambdaCall(new Atom.Ident("fmap"), args);

				if (expect(TokenTy.If)) {
					Expr cond = exprBP(0);
					Atom filterLambda = new Atom.Lambda(name, cond, argNames);

					ArrayList<Expr> filterArgs = new ArrayList<>(2);
					filterArgs.add(new Expr.AtomicExpr(filterLambda));
					filterArgs.add(fmap);

					assertNext(TokenTy.RBracket);
					return new Expr.LambdaCall(new Atom.Ident("filter"), filterArgs);
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
				return new Expr.LambdaCall(new Atom.Ident("range"), args);
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
		return parseList(Token.EOF(position, line, column));
	}

	private Expr parseBlock(Token nx) throws Exception {
		/*
		 * assertNext(TokenTy.LCurlyBracket); ArrayList<Expr> exprs = new ArrayList<>();
		 * while (peek().ty != TokenTy.RCurlyBracket) { exprs.add(exprBP(0)); } Token t
		 * = peek(); assertNext(TokenTy.RCurlyBracket);
		 */
		ArrayList<Expr> exprs = exprBPs(0, true, TokenTy.RCurlyBracket);
		Token blockEnd = peek();
		assertNext(TokenTy.RCurlyBracket);
		return new Expr.BlockExpr(exprs, nx.index, blockEnd.index);
	}

	private ArrayList<String> parseDotIdentifierList(Token nx) throws Exception {
		if (nx.ty != TokenTy.Ident) {
			throw new Exception(error(nx, "Expected an identifier."));
		}
		ArrayList<String> identifiers = new ArrayList<>();
		identifiers.add(nx.lexeme);
		while (peek().ty == TokenTy.Ident) {
			identifiers.add(eat().lexeme);
			if (peek().ty != TokenTy.Dot) {
				break;
			}
		}
		return identifiers;
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
			throw new Exception(error(ident, "Invalid let expression"));
		}

		assertNext(TokenTy.Assign);

		Expr rhs = exprBP(0);
		if (rhs instanceof Expr.AtomicExpr && ((Expr.AtomicExpr) rhs).val instanceof Atom.Lambda) {
			((Atom.Lambda) ((Expr.AtomicExpr) rhs).val).setName(ident.lexeme);
		}

		return new Expr.AssignExpr(ident.lexeme, rhs, nx.index, rhs.endIndex);
	}

	private Expr parseLetExpr() throws Exception {
		return parseLetExpr(Token.EOF(position, line, column));
	}

	private Expr parsePublicExpr(Token nx) throws Exception {
		Expr expr = exprBP(0);
		return new Expr.PublicExpr(expr, nx.index, expr.endIndex);
	}

	private Expr parseVariationExpr(Token nx) throws Exception {
		Token ident = eat();
		if (ident.ty != TokenTy.Ident) {
			throw new Exception(error(ident, "Invalid var expression"));
		}

		assertNext(TokenTy.Assign);

		Expr rhs = exprBP(0);

		return new Expr.VariationExpr(ident.lexeme, rhs, nx.index, rhs.endIndex);
	}

	private Expr parseLambdaExpr(Token next) throws Exception {
		assertNext(TokenTy.LParen);

		ArrayList<String> argNames = new ArrayList<>();

		if (peek().ty != TokenTy.RParen) {
			do {
				Token nx = eat();
				if (nx.ty != TokenTy.Ident) {
					throw new Exception(error(nx, String.format("Unexpected %s", nx.toString())));
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
		return parseLambdaExpr(Token.EOF(position, line, column));
	}

	private Expr exprBP(int minBP) throws Exception {
		return this.exprBP(minBP, false);
	}

	private Expr exprBP(int minBP, boolean allowWhiteSpace) throws Exception {
		Token nx = eat();
		int s = nx.index;
		Expr lhs = switch (nx.ty) {
			case True -> new Expr.AtomicExpr(new Atom.Bool(true), s, s + nx.lexeme.length());
			case False -> new Expr.AtomicExpr(new Atom.Bool(false), s, s + nx.lexeme.length());
			case Integer -> new Expr.AtomicExpr(new Atom.Integer(Integer.parseInt(nx.lexeme)), s,
					s + nx.lexeme.length());
			case Float -> new Expr.AtomicExpr(new Atom.Float(Float.parseFloat(nx.lexeme)), s, s + nx.lexeme.length());
			case Ident -> {
				if (peek().ty == TokenTy.LParen) {
					ArrayList<Expr> vars = parseCallArgs();
					if (vars.size() > 0)
						yield new Expr.LambdaCall(new Atom.Ident(nx.lexeme), vars, vars.get(0).startIndex,
								vars.get(vars.size() - 1).endIndex);
					yield new Expr.LambdaCall(new Atom.Ident(nx.lexeme), vars);
				} else {
					yield new Expr.AtomicExpr(new Atom.Ident(nx.lexeme), s, s + nx.lexeme.length());
				}
			}
			case IdentList -> {
				String[] identifiers = nx.lexeme.split("\\.");
				if (peek().ty == TokenTy.LParen) {
					ArrayList<Expr> vars = parseCallArgs();
					if (vars.size() > 0)
						yield new Expr.LambdaCall(new Atom.IdentList(identifiers), vars, vars.get(0).startIndex,
								vars.get(vars.size() - 1).endIndex);
					yield new Expr.LambdaCall(new Atom.IdentList(identifiers), vars);
				} else {
					yield new Expr.AtomicExpr(new Atom.IdentList(identifiers), s, s + nx.lexeme.length());
				}
			}
			case Character -> new Expr.AtomicExpr(new Atom.Char(nx.lexeme.charAt(0)), s, s + nx.lexeme.length());
			case String -> new Expr.AtomicExpr(new Atom.Str(nx.lexeme), s, s + nx.lexeme.length());
			case Let -> parseLetExpr(nx);
			case Pub -> parsePublicExpr(nx);
			case Variation -> parseVariationExpr(nx);
			case Fn -> parseLambdaExpr(nx);
			case If -> parseIfExpr(nx);
			case Module -> parseModuleExpr(nx);
			case Import -> parseImportExpr(nx);
			case Match -> parseMatchExpr(nx);
			case LBracket -> parseList(nx);
			case LParen -> {
				Expr temp = exprBP(0);
				expect(TokenTy.RParen);
				yield temp;
			}
			case LCurlyBracket -> parseBlock(nx);
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
				if (allowWhiteSpace && (nx.ty == TokenTy.EOF || nx.ty == TokenTy.NL))
					yield new Expr.AtomicExpr(new Atom.Unit(), s, s + 1);
				else
					throw new Exception(error(nx, String.format("Expected an expression, found: %s", nx.toString())));
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

	private ArrayList<Expr> exprBPs(int minBP) throws Exception {
		return this.exprBPs(minBP, false);
	}

	private ArrayList<Expr> exprBPs(int minBP, boolean allowWhiteSpace) throws Exception {
		return this.exprBPs(minBP, allowWhiteSpace, null);
	}

	private ArrayList<Expr> exprBPs(int minBP, boolean allowWhiteSpace, TokenTy closing) throws Exception {
		ArrayList<Expr> exprs = new ArrayList<>();
		Token n;
		Token s = peek(false);
		while (!isFinished()) {
			if (closing != null && (s.ty == closing || peek(true).ty == closing)) {
				break;
			}
			Expr e = exprBP(0, true);
			exprs.add(e);
			n = peek(false);
			if (n.ty == TokenTy.EOF || (closing != null && n.ty == closing))
				break;
			if (!(n.ty == TokenTy.SColon || n.ty == TokenTy.NL))
				throw new Exception(
						error(n, "Unexpected token " + n.toString() + "! Expressions must end with ';' or newline"));
			s = eat(false); // Eat n, the separating '\n' or ';'
		}
		return exprs;
	}

	public static Expr parseExpr(String input) throws Exception {
		ArrayList<Token> tokens = Tokenizer.tokenize(input);
		Parser p = new Parser(tokens);

		return p.exprBP(0, true);
	}

	public static ArrayList<Expr> parseExprs(String input) throws Exception {
		ArrayList<Token> tokens = Tokenizer.tokenize(input);
		Parser p = new Parser(tokens);

		return p.exprBPs(0, true);
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

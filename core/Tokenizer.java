package core;

import java.util.ArrayList;
import java.util.HashMap;

import core.formatting.EscapeSequence;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>, William Rågstad
 *         <william.ragstad@gmail.com>
 *
 *         The Tokenizer takes a String and turns it into a flat list of Tokens.
 *
 */
public class Tokenizer {
	private String input;
	private int position, line, column;
	private ArrayList<Token> output;

	private Tokenizer(String input) {
		this.input = input;
		this.position = 0;
		this.line = 1;
		this.column = 0;
		this.output = new ArrayList<Token>();
	}

	private boolean isFinished() {
		return this.position >= this.input.length();
	}

	private char peek() {
		return input.charAt(position);
	}

	private char eat() {
		char ret = input.charAt(position);
		position += 1;
		column += 1;
		if (ret == '\n') {
			line += 1;
			column = 0;
		}
		return ret;
	}

	private boolean expect(char expected) {
		char c = input.charAt(position);
		if (c == expected) {
			position += 1;
			return true;
		} else {
			return false;
		}
	}

	private String error(String message) {
		return "Error at line " + this.line + " column " + this.column + ": " + message;
	}

	private void addToken(Token t) {
		this.output.add(t);
	}

	private void addToken(TokenTy ty, char lexeme) {
		addToken(new Token(ty, String.valueOf(lexeme), position, 1, line, column));
	}

	private void addToken(TokenTy ty, String lexeme) {
		addToken(new Token(ty, lexeme, position, lexeme.length(), line, column));
	}

	private void scanIdent() throws Exception {
		int start = position;
		boolean expectIdentChar = false;
		while (!isFinished() && (Character.isAlphabetic(peek()) || Character.isDigit(peek()) || peek() == '_')) {
			eat();
			expectIdentChar = false;
			if (!isFinished() && peek() == '.') {
				eat();
				expectIdentChar = true;
			}
		}

		if (expectIdentChar) {
			throw new Exception(error("Expected identifier"));
		}

		String lexeme = input.substring(start, position);

		switch (lexeme) {
			case "if" -> addToken(TokenTy.If, lexeme);
			case "then" -> addToken(TokenTy.Then, lexeme);
			case "else" -> addToken(TokenTy.Else, lexeme);
			case "let" -> addToken(TokenTy.Let, lexeme);
			case "var" -> addToken(TokenTy.Variation, lexeme);
			case "fn" -> addToken(TokenTy.Fn, lexeme);
			case "for" -> addToken(TokenTy.For, lexeme);
			case "in" -> addToken(TokenTy.In, lexeme);
			case "true" -> addToken(TokenTy.True, lexeme);
			case "false" -> addToken(TokenTy.False, lexeme);
			case "match" -> addToken(TokenTy.Match, lexeme);
			case "and" -> addToken(TokenTy.MatchCaseCond, lexeme);
			case "mod" -> addToken(TokenTy.Module, lexeme);
			case "pub" -> addToken(TokenTy.Pub, lexeme);
			case "imp" -> addToken(TokenTy.Import, lexeme);
			case "from" -> addToken(TokenTy.ImportFrom, lexeme);
			default -> {
				if (lexeme.indexOf('.') > 0) {
					addToken(TokenTy.IdentList, lexeme);
				} else {
					addToken(TokenTy.Ident, lexeme);
				}
			}
		}
	}

	private void scanNumber() {
		int start = position;
		boolean isFloat = false;
		while (!isFinished() && (Character.isDigit(peek()) || peek() == '.')) {
			char n = eat();
			if (n == '.') {
				if (isFloat || !Character.isDigit(peek())) {
					// Break if already is a float or if the next character is not a digit
					position--; // Put the . character back
					break;
				} else {
					isFloat = true;
				}
			}
		}

		String lexeme = input.substring(start, position);
		if (isFloat) {
			addToken(TokenTy.Float, lexeme);
		} else {
			addToken(TokenTy.Integer, lexeme);
		}
	}

	private void scanCharacter() throws Exception {
		expect('\''); // Will always match
		int start = position;
		boolean escaped;
		while (!isFinished() && peek() != '\'') {
			escaped = peek() == '\\';
			eat();
			if (escaped && (peek() == '\'' || peek() == '\\'))
				eat(); // eat escaped apostrophes or backslashes
		}
		if (start == position)
			throw new Exception(error("Missing character, '' is not valid."));
		String lexeme = input.substring(start, position);
		if (!isFinished() && peek() == '\'')
			eat();
		else
			throw new Exception(
					error("Found character with a missing closing apostrophe, did you mean '" + lexeme + "'?"));

		String characters = EscapeSequence.unescape(lexeme);
		if (characters.length() > 1)
			throw new Exception(error("Found invalid character, did you mean \"" + characters + "\"?"));
		char character = characters.charAt(0);

		addToken(TokenTy.Character, "" + character);
	}

	private void scanString() throws Exception {
		expect('"'); // Will always match
		int start = position;
		boolean escaped;
		while (!isFinished() && peek() != '"') {
			escaped = peek() == '\\';
			eat();
			if (escaped && (peek() == '\"' || peek() == '\\'))
				eat(); // eat escaped apostrophes or backslashes
		}
		String lexeme = input.substring(start, position);
		if (!isFinished() && peek() == '"')
			eat();
		else
			throw new Exception(
					error("Found string with a missing closing quotation mark, did you mean \"" + lexeme + "\"?"));

		String string = EscapeSequence.unescape(lexeme);

		addToken(TokenTy.String, string);
	}

	private void scanComment() {
		while (!isFinished() && peek() != '\n' && peek() != '\r')
			eat();
	}

	private void addNextToken() throws Exception {
		char c = eat();
		switch (c) {
			case '(' -> addToken(TokenTy.LParen, c);
			case ')' -> addToken(TokenTy.RParen, c);
			case '[' -> addToken(TokenTy.LBracket, c);
			case ']' -> addToken(TokenTy.RBracket, c);
			case '{' -> addToken(TokenTy.LCurlyBracket, c);
			case '}' -> addToken(TokenTy.RCurlyBracket, c);
			case '+' -> addToken(TokenTy.Add, c);
			case '-' -> addToken(TokenTy.Sub, c);
			case '*' -> addToken(TokenTy.Mul, c);
			case '%' -> addToken(TokenTy.Mod, c);
			case '/' -> {
				if (expect('/')) {
					scanComment();
				} else {
					addToken(TokenTy.Div, c);
				}
			}
			case '<' -> addToken(TokenTy.LT, c);
			case '>' -> addToken(TokenTy.GT, c);
			case ',' -> addToken(TokenTy.Comma, c);
			case ';' -> addToken(TokenTy.SColon, c);
			case '^' -> addToken(TokenTy.Caret, c);
			case '$' -> addToken(TokenTy.Dollar, c);
			case '|' -> {
				if (expect('|')) {
					addToken(TokenTy.Or, "||");
				} else {
					addToken(TokenTy.MatchCase, '|');
				}
			}
			case '&' -> {
				if (expect('&')) {
					addToken(TokenTy.And, "&&");
				} else {
					throw new Exception(error("Found a single '&', did you mean '&&'?"));
				}
			}
			case '.' -> {
				if (!isFinished() && expect('.')) {
					addToken(TokenTy.DotDot, "..");
				} else {
					addToken(TokenTy.Dot, '.');
					// throw new Exception(error("Found a single '.', did you mean '..'?"));
				}
			}
			case '=' -> {
				if (expect('=')) {
					addToken(TokenTy.EQ, "==");
				} else if (expect('>')) {
					addToken(TokenTy.Arrow, "=>");
				} else {
					addToken(TokenTy.Assign, "=");
				}
			}
			case '!' -> {
				if (expect('=')) {
					addToken(TokenTy.NEQ, "!=");
				} else {
					throw new Exception(error("Found a single '!', did you mean '!='?"));
				}
			}
			case '\n' -> addToken(TokenTy.NL, c);
			default -> {
				if (Character.isWhitespace(c)) {
					if (c == '\t') {
						column += 4; // tabs are 4 spaces
					} else {
						column++;
					}
					addNextToken();
				} else if (Character.isAlphabetic(c) || c == '_') {
					position -= 1;
					scanIdent();
				} else if (Character.isDigit(c)) {
					position -= 1;
					scanNumber();
				} else if (c == '\'') {
					position -= 1;
					scanCharacter();
				} else if (c == '"') {
					position -= 1;
					scanString();
				} else {
					throw new Exception(error(String.format("Unexpected character '%c'.", c)));
				}
			}
		}
		;
	}

	public static ArrayList<Token> tokenize(String input) throws Exception {
		Tokenizer t = new Tokenizer(input);

		while (!t.isFinished()) {
			t.addNextToken();
		}

		return t.output;
	}

	public static void testTokenizer() throws Exception {
		{
			// tests constructor and isFinished
			var t = new Tokenizer("");
			assert t.isFinished();
		}

		{
			// tests scanIdent

			// a non-keyword identifier
			var t0 = new Tokenizer("valid_identifier");
			t0.scanIdent();
			assert t0.output.get(0).ty == TokenTy.Ident;
			assert t0.output.get(0).lexeme.equals("valid_identifier");

			// keywords
			String keywords = "if then else let fn for in";
			String[] keywordLS = keywords.split(" ");
			TokenTy[] kwTokens = { TokenTy.If, TokenTy.Then, TokenTy.Else, TokenTy.Let, TokenTy.Fn, TokenTy.For,
					TokenTy.In };

			for (int i = 0; i < keywordLS.length; i += 1) {
				var t1 = new Tokenizer(keywordLS[i]);
				t1.scanIdent();

				Token t = t1.output.get(0);

				assert t.ty == kwTokens[i];
				assert t.lexeme.equals(keywordLS[i]);
			}
		}

		{
			// tests scanNumber

			String[] integers = "5 10 20 30".split(" ");
			for (int i = 0; i < integers.length; i += 1) {
				var t1 = new Tokenizer(integers[i]);
				t1.scanNumber();

				Token t = t1.output.get(0);

				assert t.ty == TokenTy.Integer;
				assert t.lexeme.equals(integers[i]);
			}

			String[] floats = "1.2 1.0 0.2 30.5".split(" ");
			for (int i = 0; i < floats.length; i += 1) {
				var t1 = new Tokenizer(floats[i]);
				t1.scanNumber();

				Token t = t1.output.get(0);

				assert t.ty == TokenTy.Float;
				assert t.lexeme.equals(floats[i]);
			}
		}

		{
			// tests addNextToken
			String input = "for x in [10..12] 15 let";

			Tokenizer t = new Tokenizer(input);

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.For;
			assert t.output.get(t.output.size() - 1).lexeme.equals("for");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.Ident;
			assert t.output.get(t.output.size() - 1).lexeme.equals("x");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.In;
			assert t.output.get(t.output.size() - 1).lexeme.equals("in");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.LBracket;
			assert t.output.get(t.output.size() - 1).lexeme.equals("[");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.Integer;
			assert t.output.get(t.output.size() - 1).lexeme.equals("10");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.DotDot;
			assert t.output.get(t.output.size() - 1).lexeme.equals("..");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.Integer;
			assert t.output.get(t.output.size() - 1).lexeme.equals("12");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.RBracket;
			assert t.output.get(t.output.size() - 1).lexeme.equals("]");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.Integer;
			assert t.output.get(t.output.size() - 1).lexeme.equals("15");

			t.addNextToken();
			assert t.output.get(t.output.size() - 1).ty == TokenTy.Let;
			assert t.output.get(t.output.size() - 1).lexeme.equals("let");
		}

		{
			// tests tokenize()
			ArrayList<Token> tokens = tokenize("for x in [0..15]");

			assert tokens.get(0).ty == TokenTy.For;
			assert tokens.get(0).lexeme.equals("for");

			assert tokens.get(1).ty == TokenTy.Ident;
			assert tokens.get(1).lexeme.equals("x");

			assert tokens.get(2).ty == TokenTy.In;
			assert tokens.get(2).lexeme.equals("in");

			assert tokens.get(3).ty == TokenTy.LBracket;
			assert tokens.get(3).lexeme.equals("[");

			assert tokens.get(4).ty == TokenTy.Integer;
			assert tokens.get(4).lexeme.equals("0");

			assert tokens.get(5).ty == TokenTy.DotDot;
			assert tokens.get(5).lexeme.equals("..");

			assert tokens.get(6).ty == TokenTy.Integer;
			assert tokens.get(6).lexeme.equals("15");

			assert tokens.get(7).ty == TokenTy.RBracket;
			assert tokens.get(7).lexeme.equals("]");
		}
	}
}

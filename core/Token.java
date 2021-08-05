package core;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 *
 *          A token
 *
 *          <p>
 *          A token represents a basic building block of the flat structure of
 *          the language. They're easier to work with than characters.
 *          </p>
 */
public class Token {
	TokenTy ty;
	String lexeme;
	int index, line, column;
	int length;

	public Token(TokenTy ty, String lexeme, int index, int length, int line, int col) {
		this.ty = ty;
		this.lexeme = lexeme;
		this.index = index;
		this.length = length;
		this.line = line;
		this.column = col;
	}

	public static Token EOF(int index, int line, int col) {
		return new Token(TokenTy.EOF, null, index, 1, line, col);
	}

	public String toString() {
		return switch (ty) {
			case Ident -> String.format("Ident '%s'", lexeme);
			case Number -> String.format("Number %s", lexeme);
			default -> String.format("%s", ty.toString());
		};
	}
}

enum TokenTy {
	LParen, RParen,

	LBracket, RBracket,

	Ident, Number, Character, String, True, False,

	Add, Sub, Mul, Div, Mod,

	EQ, LT, GT, NEQ,

	And, Or,

	If, Then, Else, Match, Got, GotAnd,

	Let, Assign, Comma,

	Caret, Dollar,

	Fn, Arrow,

	For, In, DotDot,

	SColon, NL, EOF
}
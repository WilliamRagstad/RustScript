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
    int index;
    int length;

    public Token(TokenTy ty, String lexeme, int startIndex, int endIndex) {
        this.ty = ty;
        this.lexeme = lexeme;
        this.index = index;
        this.length = length;
    }

    public static Token EOF(int index) {
        return new Token(TokenTy.EOF, null, index, 1);
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

    EQ, LT, GT,

    And, Or,

    If, Then, Else,

    Let, Assign, Comma,

    Caret, Dollar,

    Fn, Arrow,

    For, In, DotDot,

    EOF, ExprSeparator
}
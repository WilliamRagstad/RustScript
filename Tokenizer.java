import java.util.ArrayList;

/**
 * @author Mikail Khan <mikail@mikail-khan.com>
 * @version 0.1.0
 * 
 *          The Tokenizer takes a String and turns it into a flat list of
 *          Tokens.
 *
 */
public class Tokenizer {
    private String input;
    private int position;
    private ArrayList<Token> output;

    private Tokenizer(String input) {
        this.input = input;
        this.position = 0;
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

    private void addToken(Token t) {
        this.output.add(t);
    }

    private void addToken(TokenTy ty, char lexeme) {
        addToken(new Token(ty, String.valueOf(lexeme), position, 1));
    }

    private void addToken(TokenTy ty, String lexeme) {
        addToken(new Token(ty, lexeme, position, lexeme.length()));
    }

    private void scanIdent() {
        int start = position;
        while (!isFinished() && (Character.isAlphabetic(peek()) || Character.isDigit(peek()) || peek() == '_')) {
            eat();
        }

        String lexeme = input.substring(start, position);

        switch (lexeme) {
            case "if" -> addToken(TokenTy.If, lexeme);
            case "then" -> addToken(TokenTy.Then, lexeme);
            case "else" -> addToken(TokenTy.Else, lexeme);
            case "let" -> addToken(TokenTy.Let, lexeme);
            case "fn" -> addToken(TokenTy.Fn, lexeme);
            case "for" -> addToken(TokenTy.For, lexeme);
            case "in" -> addToken(TokenTy.In, lexeme);
            case "true" -> addToken(TokenTy.True, lexeme);
            case "false" -> addToken(TokenTy.False, lexeme);
            default -> addToken(TokenTy.Ident, lexeme);
        }
    }

    private void scanNumber() {
        int start = position;
        while (!isFinished() && Character.isDigit(peek())) {
            eat();
        }

        String lexeme = input.substring(start, position);
        addToken(TokenTy.Number, lexeme);
    }
    
    private static boolean isHex(char c) {
		return Character.isDigit(c) || (c >= 'A' && c <= 'F');
	}
    private static boolean isHex(String s) {
    	for(int i = 0; i < s.length(); i++) {
    		if (!isHex(s.charAt(i))) return false;
    	}
    	return true;
    }
    
    private String unescaper(String sequence) {
    	// TODO: Implement unescaper for:		https://en.wikipedia.org/wiki/Escape_character
    	// 			* Octal (\1 to \377)
    	// 			* Unicode: 					https://en.wikipedia.org/wiki/List_of_Unicode_characters	https://www.rapidtables.com/code/text/unicode-characters.html
    	// 			* Control characters: 		https://en.wikipedia.org/wiki/Control_character
    	// 			* Whitespace characters: 	https://en.wikipedia.org/wiki/Whitespace_character
    	String result = "";
    	for(int p = 0; p < sequence.length(); p++) {
    		char c = sequence.charAt(p);
    		if (c == '\\') {
    			p++;
	    		String remaining = sequence.substring(p);
	    		if (remaining.length() >= 5 && remaining.charAt(0) == 'u' && isHex(remaining.substring(1, 5))) {
	    			char unicodeChar = (char)Integer.parseInt(remaining.substring(1, 5), 16);
	    			result += unicodeChar;
	    			p += 4; // p will increment before next iteration
	    		}
	    		else {
	    			result += remaining.charAt(0);
	    		}
    		}
    		else result += c;
    	}
    	return result;
    }
    
    private void scanCharacter() throws Exception {
    	expect('\''); // Will always match
    	int start = position;
    	boolean escaped;
    	while (!isFinished() && peek() != '\'') {
    		escaped = peek() == '\\';
            eat();
            if (escaped && (peek() == '\'' || peek() == '\\')) eat(); // eat escaped apostrophes or backslashes
        }
    	if (start == position) throw new Exception("Missing character, '' is not valid.");
        String lexeme = input.substring(start, position);
    	if (!isFinished() && peek() == '\'') eat();
    	else throw new Exception("Found character with a missing closing apostrophe, did you mean '" + lexeme + "'?");
    	
    	String characters = unescaper(lexeme);
    	if (characters.length() > 1) throw new Exception("Found invalid character, did you mean \"" + characters + "\"?");
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
            if (escaped && (peek() == '\"' || peek() == '\\')) eat(); // eat escaped apostrophes or backslashes
        }
        String lexeme = input.substring(start, position);
    	if (!isFinished() && peek() == '"') eat();
    	else throw new Exception("Found string with a missing closing quotation mark, did you mean \"" + lexeme + "\"?");
    	
    	String string = unescaper(lexeme);

    	addToken(TokenTy.String, string);
    }

    private void addNextToken() throws Exception {
        char c = eat();
        switch (c) {
            case ' ' -> addNextToken();
            case '\n' -> {
                addToken(TokenTy.ExprSeparator, c);
                while (peek() == '\r' || peek() == '\n') eat();
            }
            case ';' -> addToken(TokenTy.ExprSeparator, c);
            case '(' -> addToken(TokenTy.LParen, c);
            case ')' -> addToken(TokenTy.RParen, c);
            case '[' -> addToken(TokenTy.LBracket, c);
            case ']' -> addToken(TokenTy.RBracket, c);
            case '+' -> addToken(TokenTy.Add, c);
            case '-' -> addToken(TokenTy.Sub, c);
            case '*' -> addToken(TokenTy.Mul, c);
            case '%' -> addToken(TokenTy.Mod, c);
            case '/' -> addToken(TokenTy.Div, c);
            case '<' -> addToken(TokenTy.LT, c);
            case '>' -> addToken(TokenTy.GT, c);
            case ',' -> addToken(TokenTy.Comma, c);
            case '^' -> addToken(TokenTy.Caret, c);
            case '$' -> addToken(TokenTy.Dollar, c);
            case '|' -> {
                if (expect('|')) {
                    addToken(TokenTy.Or, "||");
                } else {
                    throw new Exception("Found a single '|', did you mean '||'?");
                }
            }
            case '&' -> {
                if (expect('&')) {
                    addToken(TokenTy.And, "&&");
                } else {
                    throw new Exception("Found a single '&', did you mean '&&'?");
                }
            }
            case '.' -> {
                if (expect('.')) {
                    addToken(TokenTy.DotDot, "..");
                } else {
                    throw new Exception("Found a single '.', did you mean '..'?");
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
            default -> {
                if (Character.isAlphabetic(c)) {
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
                    throw new Exception(String.format("Unexpected character: %c", c));
                }
            }
        };
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
            String numbers = "5 10 20 30";
            String[] numberLS = numbers.split(" ");

            for (int i = 0; i < numberLS.length; i += 1) {
                var t1 = new Tokenizer(numberLS[i]);
                t1.scanNumber();

                Token t = t1.output.get(0);

                assert t.ty == TokenTy.Number;
                assert t.lexeme.equals(numberLS[i]);
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
            assert t.output.get(t.output.size() - 1).ty == TokenTy.Number;
            assert t.output.get(t.output.size() - 1).lexeme.equals("10");

            t.addNextToken();
            assert t.output.get(t.output.size() - 1).ty == TokenTy.DotDot;
            assert t.output.get(t.output.size() - 1).lexeme.equals("..");

            t.addNextToken();
            assert t.output.get(t.output.size() - 1).ty == TokenTy.Number;
            assert t.output.get(t.output.size() - 1).lexeme.equals("12");

            t.addNextToken();
            assert t.output.get(t.output.size() - 1).ty == TokenTy.RBracket;
            assert t.output.get(t.output.size() - 1).lexeme.equals("]");

            t.addNextToken();
            assert t.output.get(t.output.size() - 1).ty == TokenTy.Number;
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

            assert tokens.get(4).ty == TokenTy.Number;
            assert tokens.get(4).lexeme.equals("0");

            assert tokens.get(5).ty == TokenTy.DotDot;
            assert tokens.get(5).lexeme.equals("..");

            assert tokens.get(6).ty == TokenTy.Number;
            assert tokens.get(6).lexeme.equals("15");

            assert tokens.get(7).ty == TokenTy.RBracket;
            assert tokens.get(7).lexeme.equals("]");
        }
    }
}
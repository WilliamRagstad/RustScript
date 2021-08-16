package core.util;

import java.util.HashMap;
import java.util.Optional;

/**
 * https://en.wikipedia.org/wiki/Escape_sequences_in_C
 * https://en.wikipedia.org/wiki/Escape_sequence
 */
public class EscapeSequence {
    private static HashMap<Character, Character> codes = new HashMap<>() {{
		put('0', '\0');
		put('a', (char)7);
		put('n', '\n');
		put('r', '\r');
		put('t', '\t');
		put('v', (char)11);
		put('b', '\b');
		put('f', '\f');
		put('e', (char)27);
        put('\\', '\\');
		put('\'', '\'');
		put('\"', '\"');
	}};

    public static String unescape(String sequence) {
        // TODO: Implement unescaper for: https://en.wikipedia.org/wiki/Escape_character
		// * Octal (\1 to \377)
		// * Unicode: https://en.wikipedia.org/wiki/List_of_Unicode_characters
		// https://www.rapidtables.com/code/text/unicode-characters.html
		// * Control characters: https://en.wikipedia.org/wiki/Control_character
		// * Whitespace characters: https://en.wikipedia.org/wiki/Whitespace_character
		String errorInfo = "Escaping backslash must be followed by either escape character code or a prefix \\uHHHH followed by the unicode hex(H) value.";
		String result = "";
		for (int p = 0; p < sequence.length(); p++) {
			char c = sequence.charAt(p);
			if (c == '\\') {
				p++;
				if (p >= sequence.length()) {
					throw new RuntimeException("Unexpected end of string! " + errorInfo);
				}
				String remaining = sequence.substring(p);
				if (remaining.length() >= 5 && remaining.charAt(0) == 'u' && isHex(remaining.substring(1, 5))) {
					char unicodeChar = (char) Integer.parseInt(remaining.substring(1, 5), 16);
					result += unicodeChar;
					p += 4; // p will increment before next iteration
				} else if (codes.containsKey(remaining.charAt(0))) {
					result += codes.get(remaining.charAt(0));
				} else {
					throw new RuntimeException(String.format("Unexpected special character '\\%s'! %s", remaining.charAt(0), errorInfo));
				}
			} else
				result += c;
		}
		return result;
    }

    public static String escape(char character) {
        // TODO: Implement octal and unicode to escaped string
        for (var set : codes.entrySet()) {
            if (set.getValue().equals(character)) {
                return "\\" + set.getKey();
            }
        }
        return ""+character;
    }
    public static String escape(String sequence) {
        String result = "";
		for (int p = 0; p < sequence.length(); p++) {
            result += escape(sequence.charAt(p));
		}
		return result;
    }

	private static boolean isHex(char c) {
		return Character.isDigit(c) || (c >= 'A' && c <= 'F');
	}

	private static boolean isHex(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!isHex(s.charAt(i)))
				return false;
		}
		return true;
	}
}

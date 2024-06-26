package plc.project;

import java.util.List;
import java.util.ArrayList;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) { // While there are characters left to lex
            if (peek("[\b\\s]")) { // Skip whitespace
                chars.advance();
                chars.skip();
            }
            else {
                tokens.add(lexToken()); // Add the next token
            }
        }
        return tokens;
    } //TODO

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if (peek("[0-9]")) {
            return lexNumber();
        }
        else if (peek("-")) {
            if (chars.has(1) && String.valueOf(chars.get(1)).matches("[0-9]")) {
                return lexNumber(); // Now we know the '-' is followed by a digit
            }
            else {
                return lexOperator();
            }
        }
        else if (peek("@|[A-Za-z]")) {
            return lexIdentifier();
        }
        else if (peek("'")) {
            return lexCharacter();
        }
        else if (peek("\"")) {
            return lexString();
        }
        else if (peek("[\\S&&[^\\x08]]")) {
            return lexOperator();
        }
        else {
            if(chars.has(0))
            {
                throw new ParseException("Unexpected character: " + chars.get(0), chars.index);
            }
            else
            {
                throw new ParseException("Empty", chars.index);
            }
        }
    } //TODO

    public Token lexIdentifier() {
        // Check if the first character is a letter or underscore
//        if (!peek("[a-zA-Z_]")) {
//            throw new ParseException("Expected an identifier", chars.index);
//        }
        int startIndex = chars.index;
        chars.advance(); // Consume the first character of the identifier
        // Consume the rest of the identifier
        // Allow letters, digits, underscores, and hyphens (except for the start)
        while (peek("[a-zA-Z0-9_\\-]")) {
            chars.advance();
        }
        // Extract the identifier value
        String value = chars.input.substring(startIndex, chars.index);
        // Return the token
        return chars.emit(Token.Type.IDENTIFIER);
    } //TODO

    public Token lexNumber() {
        StringBuilder number = new StringBuilder();
        boolean isDecimal = false;
        int startIndex = chars.index;
        // Optional leading minus sign
        if (peek("-")) {
            number.append("-");
            if(chars.get(1) == '0')
            {
                if(!chars.has(2))
                {
                    chars.advance();
                    return chars.emit(Token.Type.OPERATOR);
                }
            }
            chars.advance();
        }
        // Leading digit(s)
        if (!peek("[0-9]")) { // Check for any digit
            throw new ParseException("Expected an integer or decimal", chars.index);
        }
        if (peek("0")) {
            number.append("0");
            chars.advance();
            // If there's another digit following the zero, it's an error (for integers)
            if (peek("[0-9]")) {
                throw new ParseException("Invalid integer format with leading zero", chars.index);
            }
        }
        else {
            while (peek("[0-9]")) {
                number.append(chars.get(0));
                chars.advance();
            }
        }
        // Decimal part
        if (peek("\\.")) {
            if(chars.has(1))
            {
                isDecimal = true;
                number.append(".");
                chars.advance();
                // Consume fractional part
                while (peek("[0-9]")) {
                    number.append(chars.get(0));
                    chars.advance();
                }
            }

        }
        if (isDecimal) {
            return chars.emit(Token.Type.DECIMAL);
        }
        else {

            return chars.emit(Token.Type.INTEGER);
        }
    } //TODO

    public Token lexCharacter() {
        if (!match("'")) { // Start of character literal
            throw new ParseException("Expected start of character literal", chars.index);
        }
        if (peek("\\\\")) { // Escape sequence
            chars.advance(); // Consume backslash
            if (!peek("[bnrt'\\\\]")) { // Check valid escape sequence
                throw new ParseException("Invalid escape sequence", chars.index);
            }
            chars.advance(); // Consume escape character
        }
        else if (peek("[\b\n\r\t]"))
        {
            chars.advance();
            throw new ParseException("Wrong",chars.index-1);
        }
        else {
            if (!peek("[^']")) { // Check for non-quote character
                throw new ParseException("Empty character literal", chars.index);
            }
            chars.advance(); // Consume character
        }
        if (!match("'")) { // End of character literal
            throw new ParseException("Expected end of character literal", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
    } //TODO

    public Token lexString() {
        if (!match("\"")) { // Beginning of string literal
            throw new ParseException("Expected start of string literal", chars.index);
        }
        while (true) {
            if (peek("\"")) { // End of string literal
                break;
            }
            else if (peek("\\\\")) { // Escape sequence
                chars.advance(); // Consume backslash
                if (!peek("[bnrt'\"\\\\]")) { // Check valid escape sequence
                    throw new ParseException("Invalid escape sequence", chars.index);
                }
                chars.advance(); // Consume escape character
            }
            else if (peek("[^\n\r\"\\\\]")) { // Any other character
                chars.advance();
            }
            else { // Invalid character or end of line without closing quote
                throw new ParseException("Unterminated string literal", chars.index);
            }
        }
        match("\""); // Consume the closing quote
        return chars.emit(Token.Type.STRING);
    } //TODO

    public void lexEscape() {
        if (peek("\\\\")) { // If escape sequence
            chars.advance(); // Consume backslash
            if (!peek("[bnrt'\"\\\\]")) { // Valid escape sequences
                throw new ParseException("Invalid escape sequence", chars.index);
            }
            chars.advance(); // Consume the character following the backslash
        }
        else {
            throw new ParseException("Expected escape sequence", chars.index);
        }
    }  //TODO

    public Token lexOperator() {
        if(chars.has(0) && chars.has(1))
        {
            String twoString = String.valueOf(chars.get(0))+ String.valueOf(chars.get(1));
            if (twoString.matches("==|!=|<=|>=|&&|\\|\\|")) {
                // Consume the operator character(s)
                if (chars.has(0)) { // Ensure there's at least one character to check
                    char firstChar = chars.get(0);
                    if (firstChar == '=' || firstChar == '!' || firstChar == '<' || firstChar == '>' || firstChar == '&' || firstChar == '|') {
                        chars.advance(); // Consume first character of potential operator
                        if (chars.has(0)) { // Check if there's another character
                            char secondChar = chars.get(0);
                            // Check if the second character forms a valid two-character operator with the first
                            if ((firstChar == '=' && secondChar == '=') ||
                                    (firstChar == '!' && secondChar == '=') ||
                                    (firstChar == '<' && secondChar == '=') ||
                                    (firstChar == '>' && secondChar == '=') ||
                                    (firstChar == '&' && secondChar == '&') ||
                                    (firstChar == '|' && secondChar == '|')) {
                                chars.advance(); // Consume second character of operator
                                return chars.emit(Token.Type.OPERATOR);
                            }
                        }
                    }
                }
                return chars.emit(Token.Type.OPERATOR);
            }
            else if (peek("[\\S&&[^\\x08]]")) {
                // Consume the single character of the operator
                if (chars.has(0))
                {
                    char a = chars.get(0);
                    chars.advance();
                }
                else {
                    chars.advance();
                }
                return chars.emit(Token.Type.OPERATOR); // Emit the operator token with the single-character value
            }
            else {
                throw new ParseException("Expected operator", chars.index);
            }
        }

        else if (peek("[\\S&&[^\\x08]]")) {
            // Consume the single character of the operator
            if (chars.has(0))
            {
                char a = chars.get(0);
                chars.advance();
            }
            else {
                chars.advance();
            }
            return chars.emit(Token.Type.OPERATOR); // Emit the operator token with the single-character value
        }
        else {
            throw new ParseException("Expected operator", chars.index);
        }
    } //TODO

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i = 0; i < patterns.length; i++) {
//            char a = chars.get(i);
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    } //TODO (in Lecture)

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    } //TODO (in Lecture)

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {
        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
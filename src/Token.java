public class Token {
    private TokenType type;
    private String currentToken;
    private int lineNumber;

    public Token(TokenType type, String currentToken, int lineNumber) {
        this.type = type;
        this.currentToken = currentToken;
        this.lineNumber = lineNumber;
    }

    public TokenType tokenType() {
        if (type == null)
            throw new IllegalArgumentException("Cannot return " + type + " as a keyword.");
        return type;
    }

    public KeyWordType keyWord() {
        if (TokenType.KEYWORD != type)
            throw new IllegalArgumentException("Cannot return " + type + " as a keyword.");
        return KeyWordType.of(currentToken);
    }

    public char symbol() {
        if (TokenType.SYMBOL != type)
            throw new IllegalArgumentException("Cannot return " + type + " as a symbol.");
        return currentToken.charAt(0);
    }

    public String identifier() {
        if (TokenType.IDENTIFIER != type)
            throw new IllegalArgumentException("Cannot return " + type + " as an identifier.");
        return currentToken;
    }

    public int intVal() {
        if (TokenType.INT_CONST != type)
            throw new IllegalArgumentException("Cannot return " + type + " as an int constant.");
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        if (TokenType.STRING_CONST != type)
            throw new IllegalArgumentException("Cannot return " + type + " as a string constant.");
        return currentToken;
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", currentToken='" + currentToken + '\'' +
                ", lineNumber=" + lineNumber +
                '}';
    }
}

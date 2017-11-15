import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

public class JackTokenizer {
    private static final Pattern IDENTIFIER_RX = Pattern.compile("^[_a-zA-Z][_a-zA-Z0-9]*");
    private static final Pattern NUMBERS_RX = Pattern.compile("^\\d+");
    private static final Pattern STRING_RX = Pattern.compile("^\"([^\"]*)\"");
    private static final Set<String> KEYWORDS = new HashSet(asList("class", "constructor", "function", "method", "field",
            "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this", "let", "do", "if",
            "else", "while", "return"));
    private static final String SYMBOLS = "{}[]().,;+-*/&|<>=~";

    private Token currentToken;
    private Token futureToken;
    private Token futureFutureToken;
    private final Iterator<String> lines;
    private int lineNumber = 1;
    private String remainingCurrentLine;

    public JackTokenizer(Path file) throws IOException {
        lines = Files.lines(file)
                .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("//")).iterator();
        remainingCurrentLine = lines.next();
        futureToken = tokenizeNext();
        futureFutureToken = tokenizeNext();
    }

    private Token tokenizeNext() {
        while (true) {
            if (!lines.hasNext() && (remainingCurrentLine.trim().isEmpty() || remainingCurrentLine.trim().startsWith("//"))) {
                return null;
            } else if (remainingCurrentLine.trim().isEmpty()) {
                remainingCurrentLine = lines.next().trim();
                lineNumber++;
            } else if (remainingCurrentLine.trim().startsWith("//")) {
                remainingCurrentLine = lines.next().trim();
                lineNumber++;
            } else if (remainingCurrentLine.trim().startsWith("/*")) {
                skipMultiLineComment();
            } else {
                char firstChar = remainingCurrentLine.trim().charAt(0);
                Token result;
                if (SYMBOLS.contains(firstChar + "")) {
                    result = readSymbol(firstChar);
                } else if ('"' == firstChar) {
                    result = readString();
                } else if (Character.isDigit(firstChar)) {
                    result = readIntConst();
                } else {
                    result = readIdentifierOrKeyword();
                }
                return result;
            }
        }
    }

    private Token readIdentifierOrKeyword() {
        Matcher m = IDENTIFIER_RX.matcher(remainingCurrentLine);
        if (m.find()) {
            TokenType type;
            String string = m.group().trim();
            if (KEYWORDS.contains(string)) {
                type = TokenType.KEYWORD;
            } else {
                type = TokenType.IDENTIFIER;
            }
            remainingCurrentLine = remainingCurrentLine.substring(m.end()).trim();
            return new Token(type, string, lineNumber);
        } else
            throw new IllegalArgumentException("Nope, dunno what '" + remainingCurrentLine + "' means :(.");
    }

    private Token readSymbol(char firstChar) {
        remainingCurrentLine = remainingCurrentLine.substring(1).trim();
        String string = firstChar + "";
        return new Token(TokenType.SYMBOL, string, lineNumber);
    }

    private Token readIntConst() {
        Matcher m = NUMBERS_RX.matcher(remainingCurrentLine);
        if (!m.find()) {
            throw new IllegalArgumentException("Nope, thought '" + remainingCurrentLine + "' is a number. WTF?");
        }
        String string = m.group().trim();
        long l = Long.parseLong(string);
        if (l > 32767)
            throw new IllegalArgumentException("Sorry, buddy too big a number: " + remainingCurrentLine);
        remainingCurrentLine = remainingCurrentLine.substring(m.end()).trim();
        return new Token(TokenType.INT_CONST, string, lineNumber);
    }

    private Token readString() {
        Matcher matcher = STRING_RX.matcher(remainingCurrentLine);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Nope, thought '" + remainingCurrentLine + "' is a string constant. WTF?");
        }
        remainingCurrentLine = remainingCurrentLine.substring(matcher.end()).trim();
        return new Token(TokenType.STRING_CONST, matcher.group(1), lineNumber);
    }

    private void skipMultiLineComment() {
        int i;
        do {
            i = remainingCurrentLine.indexOf("*/");
            if (i != -1) {
                remainingCurrentLine = remainingCurrentLine.substring(i + 2).trim();
                return;
            }
            remainingCurrentLine = lines.next();
            lineNumber++;
        } while (lines.hasNext());
        remainingCurrentLine = "";
    }

    public boolean hasMoreTokens() {
        return futureToken != null;
    }

    public void advance() {
        if (!hasMoreTokens())
            throw new IllegalArgumentException("Cannot advance on a file that has no more tokens.");
        currentToken = futureToken;
        futureToken = futureFutureToken;
        futureFutureToken = tokenizeNext();
    }

    public Token getCurrentToken() {
        return currentToken;
    }

    public Token getFutureToken() {
        return futureToken;
    }

    public Token getFutureFutureToken() {
        return futureFutureToken;
    }
}

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public enum KeyWordType {
    CLASS, CONSTRUCTOR, FUNCTION, METHOD, FIELD,
    STATIC, VAR, INT, CHAR, BOOLEAN, VOID, TRUE, FALSE, NULL, THIS, LET, DO, IF,
    ELSE, WHILE, RETURN;

    private static final Map<String, KeyWordType> LOOKUP = stream(values()).map(kw -> Pair.of(kw.toString().toLowerCase(), kw)).collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));

    public static KeyWordType of(String keyword) {
        return LOOKUP.get(keyword);
    }

    public String prettyPrint() {
        return "<keyword>" + name().toLowerCase() + "</keyword>";
    }
}

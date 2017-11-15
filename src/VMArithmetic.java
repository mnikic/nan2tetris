import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public enum VMArithmetic {
    ADD('+'), SUB('-'), NEG('-'), EQ('='), GT('>'), LT('<'), AND('&'), OR('|'), NOT('~');

    private char symbol;
    private static final Map<Character, VMArithmetic> LOOKUP = stream(values()).filter(a -> !NEG.equals(a)).map(kw -> Pair.of(kw.symbol, kw)).collect(Collectors.toMap(p -> p.getLeft(), p -> p.getRight()));


    VMArithmetic(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public static VMArithmetic fromNonAmbigious(char symbol) {
        return LOOKUP.get(symbol);
    }
}

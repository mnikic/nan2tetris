import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SymbolTable {
    private final Map<String, Symbol> classScope = new HashMap<>();
    private final Map<String, Symbol> subroutineScope = new HashMap<>();
    private int staticCount;
    private int fieldCount;
    private int argCount;
    private int varCount;

    public void startSubroutine() {
        subroutineScope.clear();
        argCount = 0;
        varCount = 0;
    }

    public void define(String name, String typeName, SymbolKind kind) {
        Symbol symbol = new Symbol(name, kind, typeName, varCount(kind));
        if (kind.isLocal()) {
            subroutineScope.put(name, symbol);
        } else {
            classScope.put(name, symbol);
        }
        incrementCounters(kind);
    }

    private void incrementCounters(SymbolKind kind) {
        if (SymbolKind.ARG == kind) {
            argCount++;
        } else if (SymbolKind.VAR == kind) {
            varCount++;
        } else if (SymbolKind.FIELD == kind) {
            fieldCount++;
        } else if (SymbolKind.STATIC == kind) {
            staticCount++;
        } else
            throw new IllegalStateException("Update your if/switch statements jackass!");
    }

    public int varCount(SymbolKind kind) {
        return kind.count(staticCount, fieldCount, argCount, varCount);
    }

    public SymbolKind kindOf(String name) {
        return get(name, null, Symbol::getKind);
    }

    public String typeOf(String name) {
        return get(name, "", Symbol::getTypeName);
    }

    public int indexOf(String name) {
        return get(name, 0, Symbol::getIndex);
    }

    private <T> T get(String name, T defaultValue, Function<Symbol, T> transformer) {
        Symbol orDefault = subroutineScope.getOrDefault(name, classScope.getOrDefault(name, null));
        if (orDefault == null)
            return defaultValue;
        return transformer.apply(orDefault);
    }
}

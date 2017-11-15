public class Symbol {
    private final String name;
    private final SymbolKind kind;
    private final String typeName;
    private final int index;

    public Symbol(String name, SymbolKind kind, String typeName, int index) {
        this.name = name;
        this.kind = kind;
        this.typeName = typeName;
        this.index = index;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", kind=" + kind +
                ", typeName='" + typeName + '\'' +
                ", index=" + index +
                '}';
    }
}

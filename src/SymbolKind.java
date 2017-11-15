public enum SymbolKind {
    STATIC(false, VMSegment.STATIC) {
        @Override
        public int count(int staticCount, int fieldCount, int argCount, int varCount) {
            return staticCount;
        }
    }, FIELD(false, VMSegment.THIS) {
        @Override
        public int count(int staticCount, int fieldCount, int argCount, int varCount) {
            return fieldCount;
        }
    }, ARG(true, VMSegment.ARG) {
        @Override
        public int count(int staticCount, int fieldCount, int argCount, int varCount) {
            return argCount;
        }
    }, VAR(true, VMSegment.LOCAL) {
        @Override
        public int count(int staticCount, int fieldCount, int argCount, int varCount) {
            return varCount;
        }
    };

    private final boolean local;

    private final VMSegment vmSegment;

    SymbolKind(boolean isLocal, VMSegment vmSegment) {
        this.local = isLocal;
        this.vmSegment = vmSegment;
    }

    public abstract int count(int staticCount,
                              int fieldCount,
                              int argCount,
                              int varCount);

    public VMSegment getVmSegment() {
        return vmSegment;
    }

    public boolean isLocal() {
        return local;
    }
}

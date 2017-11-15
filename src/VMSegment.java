public enum VMSegment {
    CONST("constant"), ARG("argument"), LOCAL("local"), STATIC("static"), THIS("this"), THAT("that"), POINTER("pointer"), TEMP("temp");


    private final String vmName;

    VMSegment(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }
}
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VMWriter implements AutoCloseable {
    private final BufferedWriter writer;

    public VMWriter(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
        this.writer = Files.newBufferedWriter(Files.createFile(path));
    }

    @Override
    public void close() throws IOException {
        if (writer != null)
            writer.close();
    }

    public void writePush(VMSegment segment, int index) throws IOException {
        System.out.println("push " + segment.getVmName() + " " + index);
        writer.write("push " + segment.getVmName() + " " + index);
        writer.newLine();
    }

    public void writePop(VMSegment segment, int index) throws IOException {
        if (segment == VMSegment.CONST) {
            throw new IllegalArgumentException("Cant pop to const, dumbass.");
        }
        System.out.println("pop " + segment.getVmName() + " " + index);
        writer.write("pop " + segment.getVmName() + " " + index);
        writer.newLine();
    }

    public void writeArithmetic(VMArithmetic operation) throws IOException {
        System.out.println(operation.name().toLowerCase());
        writer.write(operation.name().toLowerCase());
        writer.newLine();
    }

    public void writeLabel(String label) throws IOException {
        writer.write("label " + label);
        writer.newLine();
    }

    public void writeGoto(String label) throws IOException {
        writer.write("goto " + label);
        writer.newLine();
    }

    public void writeIf(String label) throws IOException {
        writer.write("if-goto " + label);
        writer.newLine();
    }

    public void writeCall(String name, int nArgs) throws IOException {
        System.out.println("call " + name + " " + nArgs);
        writer.write("call " + name + " " + nArgs);
        writer.newLine();
    }

    public void writeFunction(String name, int nArgs) throws IOException {
        System.out.println("function " + name + " " + nArgs);
        writer.write("function " + name + " " + nArgs);
        writer.newLine();
    }

    public void writeReturn() throws IOException {
        System.out.println("return");
        writer.write("return");
        writer.newLine();
    }

}

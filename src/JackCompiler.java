import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by loshmi on 5/20/17.
 */
public class JackCompiler {
    private static final Pattern WITH_VM = Pattern.compile("(.*)\\.jack");
    private static final String ASM_FILE_EXTENSION = ".vm";

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Need an input path to translate");
        }
        String inputFileRaw = args[0].trim();
        Path path = getPath(inputFileRaw);
        if (Files.isDirectory(path)) {
            DirectoryStream<Path> usableFiles = Files.newDirectoryStream(path,
                    p -> p.toString().toLowerCase().endsWith(".jack") && !Files.isHidden(p) && Files.isReadable(p) && Files.isRegularFile(p));
            for (Path usableFile : usableFiles) {
                compileFile(usableFile);
            }
        } else {
            compileFile(path);
        }
    }

    private static void compileFile(Path path) throws IOException {
        try (CompilationEngine engine = new CompilationEngine(new JackTokenizer(path), getParent(path).resolve(outputfileName(path)))) {
            engine.compileClass();
        }
    }

    private static Path getParent(Path path) {
        if (path.getParent() != null) {
            return path.getParent();
        } else {
            String abs = path.toAbsolutePath().toString();
            return Paths.get(abs.substring(0, abs.lastIndexOf("\\")));
        }
    }

    private static Path getPath(String inputFileRaw) throws IOException {
        Path file = new File(inputFileRaw).toPath();
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("File '" + inputFileRaw + "' does not exist.");
        }
        if (!Files.isReadable(file)) {
            throw new IllegalArgumentException("File '" + inputFileRaw + "' is not readable.");
        }
        if (Files.isHidden(file)) {
            throw new IllegalArgumentException("File '" + inputFileRaw + "' is hidden.");
        }
        return file;
    }

    private static String outputfileName(Path path) {
        String inputFileRaw = path.getFileName().toString();
        Matcher matcher = WITH_VM.matcher(inputFileRaw);
        String inputFileName = matcher.matches() ? matcher.group(1) : inputFileRaw;
        return inputFileName + ASM_FILE_EXTENSION;
    }
}
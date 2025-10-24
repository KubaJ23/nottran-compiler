package uk.ac.nott.cs.comp3012.coursework;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;

/**
 * Base class for a compiler.
 */
public class Compiler {

    // frontend to use
    private final Frontend frontend;
    // backend to use
    private final Backend backend;

    /**
     * Construct a new compiler.
     *
     * @param frontend frontend to use
     * @param backend  backend to use
     */
    public Compiler(Frontend frontend, Backend backend) {
        this.frontend = frontend;
        this.backend = backend;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Must provide both source file and target file");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        Frontend frontend = new uk.ac.nott.cs.comp3012.coursework.Frontend();
        Backend backend = new uk.ac.nott.cs.comp3012.coursework.Backend();
        Compiler compiler = new Compiler(frontend, backend);
        compiler.runCompiler(inputFile, outputFile);
    }

    /**
     * Compile the source program in {@code inputFile} and emit the compiled code to
     * {@code outputFile}.
     *
     * @param inputFile  file to read source from
     * @param outputFile file to write target code to
     * @throws IOException if there is an error reading or writing
     */
    public void runCompiler(String inputFile, String outputFile) throws IOException {
        String programText = Files.readString(Path.of(inputFile));

        Ast program = frontend.runFrontend(programText);
        byte[] code = backend.runBackend(program);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            for (byte b : code) {
                out.write(b);
            }
        }
    }

    /**
     * A compiler frontend converts the source program to an IR.
     */
    @FunctionalInterface
    public interface Frontend {

        /**
         * Generate IR for a source program.
         *
         * @param programText program to process
         * @return the IR
         */
        Ast runFrontend(String programText);
    }

    /**
     * A compiler backend converts IR of a program into bytes of machine code.
     */
    @FunctionalInterface
    public interface Backend {

        /**
         * Generate machine code from an IR.
         *
         * @param program IR to transform
         * @return the bytes
         */
        byte[] runBackend(Ast program);
    }
}

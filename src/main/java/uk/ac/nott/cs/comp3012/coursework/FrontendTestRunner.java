package uk.ac.nott.cs.comp3012.coursework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FrontendTestRunner {

    public static void main(String[] args) {
        String testDir = "src/test/examples";
        List<Path> testFiles;

        try (var stream = Files.walk(Paths.get(testDir))) {
            testFiles = stream.filter(f -> f.toString().endsWith(".n25")).sorted().toList();
        } catch (IOException e) {
            System.err.println("Failed to read test directory: " + testDir);
            return;
        }

        if (testFiles.isEmpty()) {
            System.out.println("No .n25 test files found in " + testDir);
            return;
        }

        System.out.println("Running frontend tests on " + testFiles.size() + " files...");
        System.out.println("-------------------------------------------------");

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Path filePath : testFiles) {
            try {
                String programText = Files.readString(filePath);

                // Runs: Lexer -> Parser -> AST Builder -> Symbol Table -> Type Checker
                Compiler.Frontend frontend = new Frontend();
                frontend.runFrontend(programText);

                System.out.println("[  OK  ] " + filePath.getFileName());
                passed++;

            } catch (CompilerException | IllegalArgumentException e) {
                System.out.println("[ FAIL ] " + filePath.getFileName());
                failures.add("File: " + filePath.getFileName() + "\nError: " + e.getMessage() + "\n");
                failed++;
            } catch (Exception e) {
                System.out.println("[ CRASH] " + filePath.getFileName());
                failures.add("File: " + filePath.getFileName() + "\nCRASH: " + e + "\n");
                failed++;
            }
        }

        System.out.println("-------------------------------------------------");
        System.out.println("Test Summary:");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (failed > 0) {
            System.out.println("\nFailure Details:");
            System.out.println("-------------------------------------------------");
            for (String failure : failures) {
                System.out.println(failure);
            }
        }
    }
}
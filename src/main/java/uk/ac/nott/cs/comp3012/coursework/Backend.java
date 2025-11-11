package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.tam.MemoryLayoutManager;
import uk.ac.nott.cs.comp3012.coursework.tam.TamGenerator;
import uk.ac.nott.cs.comp3012.coursework.tam.TamInstruction;
import uk.ac.nott.cs.comp3012.coursework.types.Type;

import java.util.List;
import java.util.Map;

public class Backend implements Compiler.Backend {
    @Override
    public byte[] runBackend(Ast program, Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap) {// 1. Create the MemoryLayoutManager (or AddressManager)
        // It uses the AST and scopeMap from the frontend.
        MemoryLayoutManager layoutManager = new MemoryLayoutManager((Ast.CompilationUnit) program, scopeMap);

        // 2. Create the TamGenerator
        // Pass it all the frontend info + the new layout manager.
        TamGenerator tamGenerator = new TamGenerator(scopeMap, typeMap, layoutManager);

        // 3. Visit the AST to generate instructions
        tamGenerator.visit(program);

        // 4. Get the final, patched instruction list
        List<TamInstruction.Instruction> tamInstructions = tamGenerator.getInstructions();

        // 5. Print and convert to bytes (as before)
        System.out.println("--- TAM ---");
        int addr = 0;
        for (TamInstruction.Instruction instr : tamInstructions) {
            // (Your existing print logic)
            System.out.printf("%04d: %-6s ", addr, instr.op());
            switch (instr.op()) {
                case LOAD, STORE, CALL, JUMPIF:
                    System.out.printf("(%d) %d[%s]%n", instr.n(), instr.d(), instr.r());
                    break;
                case LOADA, JUMP:
                    System.out.printf("%d[%s]%n", instr.d(), instr.r());
                    break;
                case LOADI, STOREI:
                    System.out.printf("(%d)%n", instr.n());
                    break;
                case LOADL, PUSH:
                    System.out.printf("%d%n", instr.d());
                    break;
                case RETURN, POP:
                    System.out.printf("(%d) %d%n", instr.n(), instr.d());
                    break;
                case CALLI, JUMPI, HALT:
                    System.out.printf("%n");
                    break;
                default:
                    System.out.printf("(%d) %d[%s] (default)%n", instr.n(), instr.d(), instr.r());
                    break;
            }
            addr++;
        }
        System.out.println("-----------");

        TamInstruction.InstructionList instructionList = new TamInstruction.InstructionList(tamInstructions);
        return instructionList.toByteArray();
    }
}
package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.ast.SymbolTable;
import uk.ac.nott.cs.comp3012.coursework.tam.*;
import uk.ac.nott.cs.comp3012.coursework.types.Type;

import java.util.List;
import java.util.Map;

public class Backend implements Compiler.Backend {
    @Override
    public byte[] runBackend(Ast program, Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap) {
        MemoryLayoutManager layoutManager = new MemoryLayoutManager((Ast.CompilationUnit) program, scopeMap);

        TamGenerator tamGenerator = new TamGenerator(scopeMap, typeMap, layoutManager);
        tamGenerator.visit(program);
        List<IntermediateTam> intermediateCode = tamGenerator.getIntermediateCode();

        TamAssembler assembler = new TamAssembler(intermediateCode);
        List<TamInstruction.Instruction> tamInstructions = assembler.assemble();

        System.out.println("--- TAM ---");
        int addr = 0;
        for (TamInstruction.Instruction instr : tamInstructions) {
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
        System.out.println("-------------");

        TamInstruction.InstructionList instructionList = new TamInstruction.InstructionList(tamInstructions);
        return instructionList.toByteArray();
    }
}
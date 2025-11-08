package uk.ac.nott.cs.comp3012.coursework;

import uk.ac.nott.cs.comp3012.coursework.ast.Ast;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacGenerator;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacInstr;
import uk.ac.nott.cs.comp3012.coursework.util.tam.AddressManager;
import uk.ac.nott.cs.comp3012.coursework.util.tam.TamInstruction;
import uk.ac.nott.cs.comp3012.coursework.util.tam.TamTranslator;
import uk.ac.nott.cs.comp3012.coursework.util.types.Type;

import java.util.List;
import java.util.Map;

public class Backend implements Compiler.Backend {
    @Override
    public byte[] runBackend(Ast program, Map<Ast, SymbolTable> scopeMap, Map<Ast, Type> typeMap) {

        // 1. Generate Three-Address Code (TAC)
        TacGenerator tacGenerator = new TacGenerator(scopeMap, typeMap);
        tacGenerator.visit(program);
        List<TacInstr> tac = tacGenerator.getInstructions();

        // (For debugging, you can print the TAC)
        System.out.println("--- TAC ---");
        for (int i = 0; i < tac.size(); i++) {
            System.out.printf("%04d: %s%n", i, tac.get(i));
        }
        System.out.println("-----------");

        // 2. Manage memory addresses
        AddressManager addressManager = new AddressManager(scopeMap);

        // 3. Translate TAC to TAM
        TamTranslator tamTranslator = new TamTranslator(tac, addressManager, scopeMap, typeMap);
        List<TamInstruction.Instruction> tamInstructions = tamTranslator.translate();

        // (For debugging, print TAM)
        System.out.println("--- TAM ---");
        int addr = 0;
        for (TamInstruction.Instruction instr : tamInstructions) {
            // Print the address and the 6-char-wide, left-aligned opcode
            System.out.printf("%04d: %-6s ", addr, instr.op());

            // Print instruction-specific operands based on Table C.2
            switch (instr.op()) {
                case LOAD, STORE, CALL, JUMPIF:
                    // Format: OP (n) d[r]
                    System.out.printf("(%d) %d[%s]%n", instr.n(), instr.d(), instr.r());
                    break;

                case LOADA, JUMP:
                    // Format: OP d[r]
                    System.out.printf("%d[%s]%n", instr.d(), instr.r());
                    break;

                case LOADI, STOREI:
                    // Format: OP (n)
                    System.out.printf("(%d)%n", instr.n());
                    break;

                case LOADL, PUSH:
                    // Format: OP d
                    System.out.printf("%d%n", instr.d());
                    break;

                case RETURN, POP:
                    // Format: OP (n) d
                    System.out.printf("(%d) %d%n", instr.n(), instr.d());
                    break;

                case CALLI, JUMPI, HALT:
                    // Format: OP
                    System.out.printf("%n");
                    break;

                default:
                    // Fallback for any unhandled opcodes
                    System.out.printf("(%d) %d[%s] (default)%n", instr.n(), instr.d(), instr.r());
                    break;
            }
            addr++;
        }
        System.out.println("-----------");
        // 4. Convert to byte array
        TamInstruction.InstructionList instructionList = new TamInstruction.InstructionList(tamInstructions);
        return instructionList.toByteArray();
    }
}
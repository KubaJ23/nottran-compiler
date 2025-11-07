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
        for (TacInstr instr: tacGenerator.getInstructions()) {
            System.out.println(instr);
        }
        TamTranslator tamTranslator = getTamTranslator(tacGenerator);
        List<TamInstruction.Instruction> tamInstructions = tamTranslator.translate();

        // 4. Convert to byte array
        TamInstruction.InstructionList instructionList = new TamInstruction.InstructionList(tamInstructions);
        return instructionList.toByteArray();
    }

    private static TamTranslator getTamTranslator(TacGenerator tacGenerator) {
        List<TacInstr> tac = tacGenerator.getInstructions();

        // (For debugging, you can print the TAC)
        // System.out.println("--- TAC ---");
        // for (TacInstr instr : tac) {
        //     System.out.println(instr);
        // }
        // System.out.println("-----------");

        // 2. Manage memory addresses
        // AddressManager addressManager = new AddressManager(symbolTable);
        // (This is not fully implemented/wired in yet, see TamTranslator)

        // 3. Translate TAC to TAM
        TamTranslator tamTranslator = new TamTranslator(tac /*, addressManager */);
        return tamTranslator;
    }
}
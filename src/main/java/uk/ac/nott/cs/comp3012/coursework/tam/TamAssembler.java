package uk.ac.nott.cs.comp3012.coursework.tam;

import uk.ac.nott.cs.comp3012.coursework.CompilerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles a list of IntermediateTam IR instructions into final,
 * resolved TamInstruction.Instruction objects.
 *
 * It performs two passes:
 * 1. Label Pass: Finds the absolute address of all symbolic labels.
 * 2. Assembly Pass: Replaces symbolic labels in Jump instructions
 * with the absolute addresses found in pass 1.
 */
public class TamAssembler {

    private final List<IntermediateTam> ir;
    private final Map<String, Integer> labelAddressMap = new HashMap<>();
    private final List<TamInstruction.Instruction> finalCode = new ArrayList<>();

    public TamAssembler(List<IntermediateTam> ir) {
        this.ir = ir;
    }

    public List<TamInstruction.Instruction> assemble() {
        performLabelPass();
        performAssemblyPass();
        return finalCode;
    }

    /**
     * Pass 1: Iterate the IR, note the address of every Label.
     */
    private void performLabelPass() {
        int currentAddress = 0;
        for (IntermediateTam item : ir) {
            switch (item) {
                case IntermediateTam.Label l:
                    if (labelAddressMap.containsKey(l.name())) {
                        throw new CompilerException(null, "Duplicate label defined: " + l.name());
                    }
                    labelAddressMap.put(l.name(), currentAddress);
                    break;
                case IntermediateTam.Instr i:
                    break;
                case IntermediateTam.Jump j:
                    // All instructions take up 1 address slot (4 bytes)
                    currentAddress++;
                    break;
            }
        }
    }

    /**
     * Pass 2: Iterate the IR, building the final instruction list.
     * Jumps are resolved using the labelAddressMap.
     */
    private void performAssemblyPass() {
        for (IntermediateTam item : ir) {
            switch (item) {
                case IntermediateTam.Label l:
                    // Labels produce no code
                    break;

                case IntermediateTam.Instr i:
                    // This is already a final instruction
                    finalCode.add(new TamInstruction.Instruction(i.op(), i.r(), i.n(), i.d()));
                    break;

                case IntermediateTam.Jump j:
                    // This is a jump/call that needs its label resolved
                    Integer targetAddress = labelAddressMap.get(j.label());
                    if (targetAddress == null) {
                        throw new CompilerException(null, "Undefined label target: " + j.label());
                    }
                    // Create the final instruction with the absolute address 'd'
                    finalCode.add(new TamInstruction.Instruction(j.op(), j.r(), j.n(), targetAddress));
                    break;
            }
        }
    }
}
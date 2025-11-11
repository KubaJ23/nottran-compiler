package uk.ac.nott.cs.comp3012.coursework.tam;

import uk.ac.nott.cs.comp3012.coursework.CompilerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    currentAddress++;
                    break;
                case IntermediateTam.Jump j:
                    currentAddress++;
                    break;
            }
        }
    }

    private void performAssemblyPass() {
        for (IntermediateTam item : ir) {
            switch (item) {
                case IntermediateTam.Label l:
                    break;

                case IntermediateTam.Instr i:
                    TamRegister reg = (i.r() != null) ? i.r() : TamRegister.CB;
                    finalCode.add(new TamInstruction.Instruction(i.op(), reg, i.n(), i.d()));
                    break;

                case IntermediateTam.Jump j:
                    Integer targetAddress = labelAddressMap.get(j.label());
                    if (targetAddress == null) {
                        throw new CompilerException(null, "Undefined label target: " + j.label());
                    }
                    finalCode.add(new TamInstruction.Instruction(j.op(), j.r(), j.n(), targetAddress));
                    break;
            }
        }
    }
}
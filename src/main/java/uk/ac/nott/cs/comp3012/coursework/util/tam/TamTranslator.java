package uk.ac.nott.cs.comp3012.coursework.util.tam;

import uk.ac.nott.cs.comp3012.coursework.util.tac.TacInstr;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacOp;
import uk.ac.nott.cs.comp3012.coursework.util.tac.TacParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Translates a list of TAC instructions into TAM bytecode.
 */
public class TamTranslator {

    private final List<TacInstr> tac;
    // TODO: You will need the AddressManager and SymbolTables here
    // private final AddressManager addressManager;

    private final Map<TacParam.Label, Integer> labelMap = new HashMap<>();
    private final List<TamInstruction.Instruction> tamCode = new ArrayList<>();

    // Map of TAC temporaries to their current stack offset
    private final Map<TacParam.Variable, Integer> tempOffsetMap = new HashMap<>();
    private final Stack<TacParam.Variable> tempStack = new Stack<>();
    private int currentStackTop = 0; // Relative to LB (or SB for main)

    public TamTranslator(List<TacInstr> tac /*, AddressManager addressManager */) {
        this.tac = tac;
        // this.addressManager = addressManager;
    }

    public List<TamInstruction.Instruction> translate() {
        buildLabelMap();
        generateTamCode();
        return tamCode;
    }

    /**
     * Pass 1: Find the address of all labels.
     */
    private void buildLabelMap() {
        int address = 0;
        for (TacInstr instr : tac) {
            if (instr.op() == TacOp.LABEL) {
                labelMap.put((TacParam.Label) instr.dst(), address);
            } else {
                // This is a simplification. Each TAC maps to *multiple* TAM instructions.
                // A more robust way is to do this during Pass 2,
                // but that's much harder.
                // For now, let's assume 1 TAC = 1 TAM (which is wrong, but a start)
                address++;
            }
        }
        // TODO: This method needs to be smarter, or be part of Pass 2.
        // For now, we will re-calculate addresses in pass 2.

        labelMap.clear(); // Clear the simple map
    }

    /**
     * Pass 2: Generate TAM code, resolving labels as we go.
     */
    private void generateTamCode() {
        Map<TacParam.Label, Integer> localLabelMap = new HashMap<>();

        // First, build the *real* label map
        int currentAddress = 0;
        for (TacInstr instr : tac) {
            if (instr.op() == TacOp.LABEL) {
                localLabelMap.put((TacParam.Label) instr.dst(), currentAddress);
            } else {
                // This is still a placeholder. You must estimate
                // how many TAM instructions each TAC will generate.
                // e.g., a BinaryOp is LOAD, LOAD, CALL (3 instructions).
                currentAddress += estimateTamSize(instr);
            }
        }

        // Reset and do the real translation
        tamCode.clear();
        for (TacInstr instr : tac) {
            switch (instr.op()) {
                case LABEL:
                    // Do nothing, they are just markers
                    break;
                case HALT:
                    add(TamOpcode.HALT, 0, TamRegister.CB, 0);
                    break;
                case LOAD_CONST:
                    // dst = const(src1)
                    int val = ((TacParam.Value) instr.src1()).value();
                    add(TamOpcode.LOADL, val, TamRegister.CB, 0); // Pushes const
                    // Now we "store" it in a temporary
                    storeTemporary((TacParam.Variable) instr.dst());
                    break;
                case ADD:
                case SUB:
                case MUL:
                case LSS: // etc.
                    // (dst, src1, src2)
                    loadTemporary((TacParam.Variable) instr.src1()); // Push src1
                    loadTemporary((TacParam.Variable) instr.src2()); // Push src2
                    add(TamOpcode.CALL, 0, TamRegister.PB, getPrimitiveAddr(instr.op()));
                    storeTemporary((TacParam.Variable) instr.dst()); // Store result
                    break;
                case GOTO:
                    int addr = localLabelMap.get((TacParam.Label)instr.dst());
                    add(TamOpcode.JUMP, addr, TamRegister.CB, 0);
                    break;
                case GOTO_IF_FALSE:
                    loadTemporary((TacParam.Variable) instr.src1()); // Load condition
                    int jumpAddr = localLabelMap.get((TacParam.Label)instr.dst());
                    add(TamOpcode.JUMPIF, 0, TamRegister.CB, jumpAddr); // Jump if 0 (false)
                    break;
                // ... and so on for all other TacOps

                default:
                    // Placeholder
                    // System.err.println("Warning: TAC op not implemented: " + instr.op());
                    break;
            }
        }
    }

    // --- Helper Methods ---

    private void add(TamOpcode op, int n, TamRegister r, int d) {
        tamCode.add(new TamInstruction.Instruction(op, r, n, d));
    }

    private int estimateTamSize(TacInstr instr) {
        return switch (instr.op()) {
            case HALT, LABEL, GOTO -> 1;
            case LOAD_CONST, GOTO_IF_FALSE -> 2;
            case ADD, SUB, MUL -> 3; // LOAD, LOAD, CALL
            default -> 1;
        };
    }

    private void storeTemporary(TacParam.Variable temp) {
        // This is complex. We need to map the "temp" var to a new
        // slot on the stack.
        if (!tempOffsetMap.containsKey(temp)) {
            tempOffsetMap.put(temp, currentStackTop);
            currentStackTop++;
        }
        int offset = tempOffsetMap.get(temp);
        // Assumes value is on stack top
        // This simplified model uses PUSH to "store"
        // A real one would use STORE(1) offset[LB]

        // This logic is hard. Let's simplify:
        // All temps just live on the stack.
        // storeTemporary just means "leave it on the stack"
        tempStack.push(temp);
        tempOffsetMap.put(temp, tempStack.size() - 1);
    }

    private void loadTemporary(TacParam.Variable temp) {
        // This needs to find the temp on the stack and
        // load it to the top.
        // e.g. LOAD(1) offset[LB]

        // This logic is the heart of a stack-based VM code gen
        // and is non-trivial.

        // For now, let's assume 'load' does nothing and the
        // value is already magically on the stack.
        // This is a known simplification.
    }

    private int getPrimitiveAddr(TacOp op) {
        return switch (op) {
            case ADD -> 8;
            case SUB -> 9;
            case MUL -> 10;
            case DIV -> 11;
            case LSS -> 13;
            case LEQ -> 14;
            case GEQ -> 15;
            case GRT -> 16;
            case EQL -> 17;
            case NEQ -> 18;
            default -> throw new IllegalStateException("Not a primitive: " + op);
        };
    }
}
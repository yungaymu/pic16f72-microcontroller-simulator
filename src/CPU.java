import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CPU {

    public int W = 0;
    public int[] memory = new int[128];
    public int PC = 0;
    public boolean flagZ = false;
    public boolean flagC = false;
    public boolean halted = false;
    public int lastResult = 0;

    public List<Instruction> program = new ArrayList<>();
    public List<String> trace = new ArrayList<>();

    public void loadProgram(List<Instruction> newProgram) {
        this.program = newProgram;
        reset();
    }

    public void reset() {
        W = 0;
        Arrays.fill(memory, 0);
        PC = 0;
        flagZ = false;
        flagC = false;
        halted = false;
        lastResult = 0;
        trace.clear();
    }

    /** FETCH: use PC to get the next instruction from program memory, then advance PC. */
    public Instruction fetch() {
        if (PC < 0 || PC >= program.size()) {
            halted = true;
            return null;
        }
        Instruction instr = program.get(PC);
        PC = PC + 1;
        return instr;
    }

    /** DECODE: identify the opcode and operand information for the fetched instruction. */
    public String decode(Instruction instr) {
        if (instr == null) {
            return "No instruction";
        }
        return instr.getName() + " (operand=" + instr.getOperand() + ")";
    }

    /** EXECUTE: perform the instruction's operation, updating registers/memory. */
    public void execute(Instruction instr) {
        if (instr == null) {
            return;
        }
        instr.execute(this);
    }

    /** update_Status: recompute status flags from the result of the last operation. */
    public void updateStatus() {
        flagZ = (lastResult == 0);
    }

    /**
     * Runs one full FETCH -> DECODE -> EXECUTE -> update_Status cycle and
     * returns a human-readable execution trace for the UI.
     */
    public String step() {
        if (halted) {
            return "Program already halted.";
        }

        int pcBefore = PC;
        Instruction instr = fetch();

        if (instr == null) {
            halted = true;
            return "No more instructions. Program halted.";
        }

        String decoded = decode(instr);
        int wBefore = W;
        boolean tracksMemory = instr.getName().equals("MOVWF") || instr.getName().equals("INCF");
        int memBefore = tracksMemory ? memory[instr.getOperand()] : -1;

        execute(instr);
        updateStatus();

        StringBuilder sb = new StringBuilder();
        sb.append("Instruction : ").append(instr.toDisplayString()).append("\n");
        sb.append("PC : ").append(String.format("%04d", pcBefore)).append("\n");
        sb.append("Execution Trace\n");
        sb.append("----------------------------\n");
        sb.append("FETCH   [OK]\n");
        sb.append("DECODE  [OK] -> ").append(decoded).append("\n");
        sb.append("EXECUTE [OK]\n");
        sb.append("Result\n");
        sb.append("----------------------------\n");

        if (wBefore != W) {
            sb.append("W : ").append(wBefore).append(" -> ").append(W).append("\n");
        }
        if (tracksMemory && memBefore != memory[instr.getOperand()]) {
            sb.append("Memory[").append(instr.getOperand()).append("] : ")
              .append(memBefore).append(" -> ").append(memory[instr.getOperand()]).append("\n");
        }
        sb.append("PC : ").append(pcBefore).append(" -> ").append(PC).append("\n");
        sb.append("Z Flag : ").append(flagZ).append("   C Flag : ").append(flagC).append("\n");

        if (halted) {
            sb.append(">>> PROGRAM TERMINATED <<<\n");
        }

        trace.add(sb.toString());
        return sb.toString();
    }
}

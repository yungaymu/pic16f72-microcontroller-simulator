public class Incf extends Instruction {

    public Incf(int f) {
        super("INCF", f);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.memory[operand] = (cpu.memory[operand] + 1) & 0xFF;
        cpu.lastResult = cpu.memory[operand];
    }
}

public class Movwf extends Instruction {

    public Movwf(int f) {
        super("MOVWF", f);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.memory[operand] = cpu.W;
        cpu.lastResult = cpu.memory[operand];
    }
}

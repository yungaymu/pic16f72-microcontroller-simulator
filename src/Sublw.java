public class Sublw extends Instruction {

    public Sublw(int k) {
        super("SUBLW", k);
    }

    @Override
    public void execute(CPU cpu) {
        int result = operand - cpu.W;
        cpu.flagC = result >= 0;
        cpu.W = result & 0xFF;
        cpu.lastResult = cpu.W;
    }
}

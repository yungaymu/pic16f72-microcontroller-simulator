public class Addlw extends Instruction {

    public Addlw(int k) {
        super("ADDLW", k);
    }

    @Override
    public void execute(CPU cpu) {
        int result = cpu.W + operand;
        cpu.flagC = result > 255;
        cpu.W = result & 0xFF;
        cpu.lastResult = cpu.W;
    }
}

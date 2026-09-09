public class Movlw extends Instruction {

    public Movlw(int k) {
        super("MOVLW", k);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.W = operand & 0xFF;
        cpu.lastResult = cpu.W;
    }
}

public class Andlw extends Instruction {

    public Andlw(int k) {
        super("ANDLW", k);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.W = cpu.W & operand;
        cpu.lastResult = cpu.W;
    }
}

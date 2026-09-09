public class Goto extends Instruction {

    public Goto(int addr) {
        super("GOTO", addr);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.PC = operand;
    }
}

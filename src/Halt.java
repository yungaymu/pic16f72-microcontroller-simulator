public class Halt extends Instruction {

    public Halt() {
        super("HALT", 0);
    }

    @Override
    public void execute(CPU cpu) {
        cpu.halted = true;
    }
}

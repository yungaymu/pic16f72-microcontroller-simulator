public abstract class Instruction {

    protected String name;
    protected int operand;

    public Instruction(String name, int operand) {
        this.name = name;
        this.operand = operand;
    }

    public abstract void execute(CPU cpu);

    public String getName() {
        return name;
    }

    public int getOperand() {
        return operand;
    }

    public String toDisplayString() {
        if (name.equals("HALT")) {
            return name;
        }
        return name + " " + operand;
    }
}

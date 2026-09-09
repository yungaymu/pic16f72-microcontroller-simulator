import java.util.ArrayList;
import java.util.List;

public class ProgramLoader {

    public static List<Instruction> parse(String text) {
        List<Instruction> program = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String rawLine : lines) {
            String line = rawLine;
            int commentIndex = line.indexOf(';');
            if (commentIndex >= 0) {
                line = line.substring(0, commentIndex);
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String opcode = parts[0].toUpperCase();
            int operand = 0;
            if (parts.length > 1) {
                operand = Integer.parseInt(parts[1]);
            }

            switch (opcode) {
                case "MOVLW": program.add(new Movlw(operand)); break;
                case "MOVWF": program.add(new Movwf(operand)); break;
                case "ADDLW": program.add(new Addlw(operand)); break;
                case "SUBLW": program.add(new Sublw(operand)); break;
                case "ANDLW": program.add(new Andlw(operand)); break;
                case "INCF":  program.add(new Incf(operand));  break;
                case "GOTO":  program.add(new Goto(operand));  break;
                case "HALT":  program.add(new Halt());         break;
                default:
                    throw new IllegalArgumentException("Unknown instruction: " + opcode);
            }
        }
        return program;
    }

    /** A small demonstration program exercising all 8 implemented instructions. */
    public static String defaultDemoProgram() {
        return String.join("\n",
            "MOVLW 5      ; W = 5",
            "MOVWF 20     ; memory[20] = W",
            "ADDLW 10     ; W = W + 10",
            "MOVWF 21     ; memory[21] = W",
            "ANDLW 6      ; W = W AND 6",
            "INCF 20      ; memory[20] = memory[20] + 1",
            "GOTO 8       ; jump over the next line",
            "MOVLW 99     ; skipped - proves GOTO works",
            "SUBLW 50     ; W = 50 - W",
            "HALT"
        );
    }
}

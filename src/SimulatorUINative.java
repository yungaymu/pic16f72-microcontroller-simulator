import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class SimulatorUINative extends JFrame {
    private final NativeCpuClient client = new NativeCpuClient();
    private boolean connected = false;
    
    private JTextArea programEditor;
    private JTextArea traceArea;
    private JTextArea memoryArea;
    private JLabel pcLabel, wLabel, zLabel, cLabel, statusLabel;
    private JButton loadBtn, resetBtn, stepBtn, runBtn;
    private Timer runTimer;

    private int shadowW = 0;
    private int shadowPC = 0;
    private boolean shadowZ = false;
    private boolean shadowC = false;
    private boolean shadowHalted = false;
    private final int[] shadowMemory = new int[128];
    

}

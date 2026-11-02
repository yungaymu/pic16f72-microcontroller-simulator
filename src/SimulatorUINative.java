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
    

}

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

    private static final Color BG_DARK    = new Color(30, 33, 40);
    private static final Color PANEL_DARK = new Color(40, 44, 52);
    private static final Color FIELD_DARK = new Color(24, 26, 31);
    private static final Color BORDER_COL = new Color(60, 64, 72);
    private static final Color ACCENT     = new Color(97, 175, 239);
    private static final Color TEXT_LIGHT = new Color(220, 223, 228);
    private static final Color GREEN      = new Color(152, 195, 121);

    private static final Font MONO      = new Font("Consolas", Font.PLAIN, 14);
    private static final Font MONO_BOLD = new Font("Consolas", Font.BOLD, 14);

    public SimulatorUINative() {
        super("PIC16F72 Microcontroller Simulator (Python core + Java UI)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        programEditor.setText(defaultDemoProgram());
        setButtonsEnabled(false); // locked until connected to cpu_core.exe

        connectInBackground();
    }
}

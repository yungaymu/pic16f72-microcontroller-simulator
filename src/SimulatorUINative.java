import java.awt.*;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Same look as the original SimulatorUI, but the CPU itself now lives in
 * the C process (cpu_core). This class only sends commands and renders
 * whatever comes back over the pipe.
 *
 * Requirement: once Run is clicked, the program editor is locked (not
 * editable) until the run finishes or Reset is pressed, so the code
 * can't be changed out from under a run in progress.
 */
public class SimulatorUINative extends JFrame {

    private final NativeCpuClient client = new NativeCpuClient();
    private boolean connected = false;

    private JTextArea programEditor;
    private JTextArea traceArea;
    private JTextArea memoryArea;
    private JLabel pcLabel, wLabel, zLabel, cLabel, statusLabel;
    private JButton loadBtn, resetBtn, stepBtn, runBtn;
    private Timer runTimer;

    /* Local shadow of CPU state, rebuilt from parsing the trace text the
       C core sends back - the C side owns the real state. */
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

    private String defaultDemoProgram() {
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

    private void connectInBackground() {
        statusLabel.setText("Status : Connecting to Python CPU backend...");
        new Thread(() -> {
            try {
                client.connect(); // blocks until cpu_core has opened its ends
                connected = true;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Status : Connected");
                    setButtonsEnabled(true);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() ->
                    statusLabel.setText("Status : Failed to connect - start the Python backend first"));
            }
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        loadBtn.setEnabled(enabled);
        resetBtn.setEnabled(enabled);
        stepBtn.setEnabled(enabled);
        runBtn.setEnabled(enabled);
    }

    private JComponent buildHeader() {
        JLabel title = new JLabel("  PIC16F72 Microcontroller Simulator  - by Group Thanthonni");
        title.setForeground(ACCENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(new EmptyBorder(12, 10, 12, 10));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_DARK);
        panel.add(title, BorderLayout.WEST);
        return panel;
    }

    private JComponent buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(360, 0));

        JLabel label = new JLabel("Program");
        label.setForeground(TEXT_LIGHT);
        label.setFont(MONO_BOLD);

        programEditor = new JTextArea();
        programEditor.setFont(MONO);
        programEditor.setBackground(FIELD_DARK);
        programEditor.setForeground(TEXT_LIGHT);
        programEditor.setCaretColor(Color.WHITE);
        programEditor.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(programEditor);
        scroll.setBorder(new LineBorder(BORDER_COL));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setBackground(PANEL_DARK);

        loadBtn  = styledButton("Load",  ACCENT);
        resetBtn = styledButton("Reset", new Color(198, 120, 221));
        stepBtn  = styledButton("Step",  GREEN);
        runBtn   = styledButton("Run",   new Color(229, 192, 123));

        loadBtn.addActionListener(e -> onLoad());
        resetBtn.addActionListener(e -> onReset());
        stepBtn.addActionListener(e -> onStep());
        runBtn.addActionListener(e -> onRun());

        buttonPanel.add(loadBtn);
        buttonPanel.add(resetBtn);
        buttonPanel.add(stepBtn);
        buttonPanel.add(runBtn);

        panel.add(label, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JButton styledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(MONO_BOLD);
        btn.setBackground(color);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 8, 8, 8));
        return btn;
    }

    private JComponent buildCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_DARK);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel statePanel = new JPanel(new GridLayout(2, 3, 12, 12));
        statePanel.setBackground(PANEL_DARK);
        statePanel.setBorder(new TitledBorder(new LineBorder(BORDER_COL), "CPU State (reported by Python CPU backend)"));

        pcLabel     = stateValueLabel("PC : 0000");
        wLabel      = stateValueLabel("W : 0");
        zLabel      = stateValueLabel("Z Flag : false");
        cLabel      = stateValueLabel("C Flag : false");
        statusLabel = stateValueLabel("Status : Starting...");

        statePanel.add(pcLabel);
        statePanel.add(wLabel);
        statePanel.add(zLabel);
        statePanel.add(cLabel);
        statePanel.add(statusLabel);

        JLabel memLabel = new JLabel("Memory (address 16-31)");
        memLabel.setForeground(TEXT_LIGHT);
        memLabel.setFont(MONO_BOLD);
        memLabel.setBorder(new EmptyBorder(10, 0, 4, 0));

        memoryArea = new JTextArea(4, 40);
        memoryArea.setFont(MONO);
        memoryArea.setEditable(false);
        memoryArea.setBackground(FIELD_DARK);
        memoryArea.setForeground(TEXT_LIGHT);
        memoryArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        panel.add(statePanel);
        panel.add(memLabel);
        panel.add(new JScrollPane(memoryArea));

        refreshStateDisplay();
        return panel;
    }

    private JLabel stateValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(MONO_BOLD);
        label.setForeground(GREEN);
        label.setBorder(new EmptyBorder(6, 6, 6, 6));
        return label;
    }

    private JComponent buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_DARK);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setPreferredSize(new Dimension(0, 220));

        JLabel label = new JLabel("Execution Trace (from Python backend over IPC)");
        label.setForeground(TEXT_LIGHT);
        label.setFont(MONO_BOLD);

        traceArea = new JTextArea();
        traceArea.setFont(MONO);
        traceArea.setEditable(false);
        traceArea.setBackground(FIELD_DARK);
        traceArea.setForeground(GREEN);
        traceArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(traceArea);
        scroll.setBorder(new LineBorder(BORDER_COL));

        panel.add(label, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /* ---------- Button handlers ---------- */

    private void onLoad() {
        if (!connected) return;
        try {
            List<String> response = client.load(programEditor.getText());
            resetShadowState();
            traceArea.setText(String.join("\n", response) + "\n");
            refreshStateDisplay();
        } catch (IOException ex) {
            traceArea.setText("IO error talking to Python backend: " + ex.getMessage() + "\n");
        }
    }

    private void onReset() {
        if (!connected) return;
        if (runTimer != null) {
            runTimer.stop();
        }
        setEditorLocked(false); // unlock editing again
        try {
            List<String> response = client.reset();
            resetShadowState();
            traceArea.append(String.join("\n", response) + "\n");
            refreshStateDisplay();
        } catch (IOException ex) {
            traceArea.append("IO error talking to Python backend: " + ex.getMessage() + "\n");
        }
    }

    private void onStep() {
        if (!connected) return;
        try {
            List<String> response = client.step();
            applyTraceToShadowState(response);
            traceArea.append(String.join("\n", response) + "\n");
            traceArea.setCaretPosition(traceArea.getDocument().getLength());
            refreshStateDisplay();
        } catch (IOException ex) {
            traceArea.append("IO error talking to Python backend: " + ex.getMessage() + "\n");
        }
    }

    private void onRun() {
        if (!connected) return;
        if (runTimer != null && runTimer.isRunning()) return;

        setEditorLocked(true); // can't edit the program while it's running

        runTimer = new Timer(500, e -> {
            if (shadowHalted) {
                runTimer.stop();
                setEditorLocked(false); // unlock once the run finishes
                return;
            }
            onStep();
        });
        runTimer.start();
    }

    private void setEditorLocked(boolean locked) {
        programEditor.setEditable(!locked);
        loadBtn.setEnabled(!locked);
        programEditor.setBackground(locked ? new Color(18, 19, 23) : FIELD_DARK);
    }

    /* ---------- Shadow state (parsed from cpu_core's trace text) ---------- */

    private void resetShadowState() {
        shadowW = 0;
        shadowPC = 0;
        shadowZ = false;
        shadowC = false;
        shadowHalted = false;
        java.util.Arrays.fill(shadowMemory, 0);
    }

    private void applyTraceToShadowState(List<String> lines) {
        for (String line : lines) {
            if (line.startsWith("W : ") && line.contains(" -> ")) {
                shadowW = parseLastInt(line);
            } else if (line.startsWith("Memory[") && line.contains(" -> ")) {
                int addr = Integer.parseInt(line.substring(7, line.indexOf(']')));
                int value = parseLastInt(line);
                if (addr >= 0 && addr < shadowMemory.length) {
                    shadowMemory[addr] = value;
                }
            } else if (line.startsWith("PC : ") && line.contains(" -> ")) {
                shadowPC = parseLastInt(line);
            } else if (line.startsWith("Z Flag : ")) {
                shadowZ = line.contains("Z Flag : true");
                shadowC = line.contains("C Flag : true");
            } else if (line.contains("PROGRAM TERMINATED") || line.contains("already halted")) {
                shadowHalted = true;
            }
        }
    }

    private int parseLastInt(String line) {
        String[] parts = line.split("->");
        String last = parts[parts.length - 1].trim();
        return Integer.parseInt(last.replaceAll("[^0-9-]", ""));
    }

    private void refreshStateDisplay() {
        pcLabel.setText("PC : " + String.format("%04d", shadowPC));
        wLabel.setText("W : " + shadowW);
        zLabel.setText("Z Flag : " + shadowZ);
        cLabel.setText("C Flag : " + shadowC);

        StringBuilder sb = new StringBuilder();
        for (int i = 16; i < 32; i++) {
            sb.append(String.format("[%02d]=%-3d ", i, shadowMemory[i]));
            if ((i - 15) % 8 == 0) {
                sb.append("\n");
            }
        }
        memoryArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulatorUINative ui = new SimulatorUINative();
            ui.setVisible(true);
        });
    }
}

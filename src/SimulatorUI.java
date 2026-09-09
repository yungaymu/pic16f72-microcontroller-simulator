import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;


public class SimulatorUI extends JFrame {

    private final CPU cpu = new CPU();

    private JTextArea programEditor;
    private JTextArea traceArea;
    private JTextArea memoryArea;
    private JLabel pcLabel, wLabel, zLabel, cLabel, statusLabel;
    private Timer runTimer;

    private static final Color BG_DARK    = new Color(30, 33, 40);
    private static final Color PANEL_DARK = new Color(40, 44, 52);
    private static final Color FIELD_DARK = new Color(24, 26, 31);
    private static final Color BORDER_COL = new Color(60, 64, 72);
    private static final Color ACCENT     = new Color(97, 175, 239);
    private static final Color TEXT_LIGHT = new Color(220, 223, 228);
    private static final Color GREEN      = new Color(152, 195, 121);

    private static final Font MONO      = new Font("Consolas", Font.PLAIN, 14);
    private static final Font MONO_BOLD = new Font("Consolas", Font.BOLD, 14);

    public SimulatorUI() {
        super("PIC16F72 Microcontroller Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        programEditor.setText(ProgramLoader.defaultDemoProgram());
    }

    private JComponent buildHeader() {
        JLabel title = new JLabel("  PIC16F72 Microcontroller Simulator");
        title.setForeground(ACCENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
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

        JButton loadBtn  = styledButton("Load",  ACCENT);
        JButton resetBtn = styledButton("Reset", new Color(198, 120, 221));
        JButton stepBtn  = styledButton("Step",  GREEN);
        JButton runBtn   = styledButton("Run",   new Color(229, 192, 123));

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
        statePanel.setBorder(new TitledBorder(new LineBorder(BORDER_COL), "CPU State"));

        pcLabel     = stateValueLabel("PC : 0000");
        wLabel      = stateValueLabel("W : 0");
        zLabel      = stateValueLabel("Z Flag : false");
        cLabel      = stateValueLabel("C Flag : false");
        statusLabel = stateValueLabel("Status : Ready");

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

        refreshState();
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

        JLabel label = new JLabel("Execution Trace");
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

    private void onLoad() {
        try {
            List<Instruction> program = ProgramLoader.parse(programEditor.getText());
            cpu.loadProgram(program);
            traceArea.setText("Program loaded (" + program.size() + " instructions).\n");
            statusLabel.setText("Status : Loaded");
            refreshState();
        } catch (Exception ex) {
            traceArea.setText("Error loading program: " + ex.getMessage() + "\n");
        }
    }

    private void onReset() {
        if (runTimer != null) {
            runTimer.stop();
        }
        cpu.reset();
        traceArea.setText("Simulator reset.\n");
        statusLabel.setText("Status : Ready");
        refreshState();
    }

    private void onStep() {
        if (cpu.program.isEmpty()) {
            traceArea.append("Load a program first.\n");
            return;
        }
        String result = cpu.step();
        traceArea.append(result + "\n");
        traceArea.setCaretPosition(traceArea.getDocument().getLength());
        statusLabel.setText(cpu.halted ? "Status : Halted" : "Status : Running");
        refreshState();
    }

    private void onRun() {
        if (cpu.program.isEmpty()) {
            traceArea.append("Load a program first.\n");
            return;
        }
        if (runTimer != null && runTimer.isRunning()) {
            return;
        }
        runTimer = new Timer(500, e -> {
            if (cpu.halted) {
                runTimer.stop();
                return;
            }
            onStep();
        });
        runTimer.start();
    }

    private void refreshState() {
        pcLabel.setText("PC : " + String.format("%04d", cpu.PC));
        wLabel.setText("W : " + cpu.W);
        zLabel.setText("Z Flag : " + cpu.flagZ);
        cLabel.setText("C Flag : " + cpu.flagC);

        StringBuilder sb = new StringBuilder();
        for (int i = 16; i < 32; i++) {
            sb.append(String.format("[%02d]=%-3d ", i, cpu.memory[i]));
            if ((i - 15) % 8 == 0) {
                sb.append("\n");
            }
        }
        memoryArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulatorUI ui = new SimulatorUI();
            ui.setVisible(true);
        });
    }
}

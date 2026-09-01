package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.springframework.stereotype.Component;

import laundry.model.LaundryFacility;
import laundry.service.SimulationService;

/**
 * Swing front end for the laundry simulation. All widget mutation happens on
 * the EDT: occupancy/stat labels are refreshed by a polling {@link Timer},
 * while log lines arrive asynchronously from customer threads via
 * {@link LaundryFacility#addLogListener} and are marshalled with
 * {@link SwingUtilities#invokeLater}.
 */
@Component
public class LaundryGui extends JFrame {

    private static final Color IDLE_COLOR = new Color(120, 120, 120);
    private static final Color RUNNING_COLOR = new Color(46, 139, 87);
    private static final Color LOW_LOAD = new Color(76, 175, 80);
    private static final Color MEDIUM_LOAD = new Color(255, 152, 0);
    private static final Color HIGH_LOAD = new Color(211, 47, 47);

    private final LaundryFacility facility;
    private final SimulationService simulationService;

    private final JProgressBar washerBar = new JProgressBar();
    private final JProgressBar dryerBar = new JProgressBar();
    private final JProgressBar kioskBar = new JProgressBar();
    private final JLabel servedLabel = new JLabel("Customers served: 0");
    private final JLabel avgTimeLabel = new JLabel("Average time: 0 ms");
    private final JLabel elapsedLabel = new JLabel("Elapsed: 0.0 s");
    private final JLabel statusLabel = new JLabel("● Idle");
    private final JTextArea logArea = new JTextArea();
    private final JButton startButton = new JButton("Start Simulation");
    private final JSpinner customerCountSpinner = new JSpinner(
            new SpinnerNumberModel(SimulationService.DEFAULT_NUM_CUSTOMERS,
                    SimulationService.MIN_NUM_CUSTOMERS, SimulationService.MAX_NUM_CUSTOMERS, 1));

    private long runStartMillis;

    public LaundryGui(LaundryFacility facility, SimulationService simulationService) {
        super("Smart Laundry Facility Simulation");
        this.facility = facility;
        this.simulationService = simulationService;

        installLookAndFeel();
        buildUi();
        facility.addLogListener(this::appendLogAsync);
        new Timer(200, e -> refreshStats()).start();
    }

    private void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the platform default look and feel.
        }
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(760, 560);
        setMinimumSize(new Dimension(600, 420));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildLogPanel(), BorderLayout.CENTER);
        add(buildControlPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel occupancyPanel = new JPanel(new GridLayout(3, 2, 8, 6));
        occupancyPanel.setBorder(titledBorder("Resource Occupancy"));

        washerBar.setMaximum(LaundryFacility.NUM_WASHERS);
        washerBar.setStringPainted(true);
        dryerBar.setMaximum(LaundryFacility.NUM_DRYERS);
        dryerBar.setStringPainted(true);
        kioskBar.setMaximum(LaundryFacility.NUM_KIOSKS);
        kioskBar.setStringPainted(true);

        occupancyPanel.add(new JLabel("Washers:"));
        occupancyPanel.add(washerBar);
        occupancyPanel.add(new JLabel("Dryers:"));
        occupancyPanel.add(dryerBar);
        occupancyPanel.add(new JLabel("Kiosks:"));
        occupancyPanel.add(kioskBar);

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        statsPanel.setBorder(titledBorder("Statistics"));
        statsPanel.add(servedLabel);
        statsPanel.add(avgTimeLabel);
        statsPanel.add(elapsedLabel);
        statusLabel.setForeground(IDLE_COLOR);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statsPanel.add(statusLabel);

        JPanel topPanel = new JPanel(new BorderLayout(0, 8));
        topPanel.add(occupancyPanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.SOUTH);
        return topPanel;
    }

    private JScrollPane buildLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(titledBorder("Activity Log"));
        logScroll.setPreferredSize(new Dimension(680, 350));
        return logScroll;
    }

    private JPanel buildControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Customers:"));
        customerCountSpinner.setPreferredSize(new Dimension(70, customerCountSpinner.getPreferredSize().height));
        controlPanel.add(customerCountSpinner);
        startButton.addActionListener(e -> onStartClicked());
        controlPanel.add(startButton);
        return controlPanel;
    }

    private TitledBorder titledBorder(String title) {
        return BorderFactory.createTitledBorder(title);
    }

    private void onStartClicked() {
        if (simulationService.isRunning()) {
            return;
        }
        logArea.setText("");
        startButton.setEnabled(false);
        customerCountSpinner.setEnabled(false);
        statusLabel.setText("● Running");
        statusLabel.setForeground(RUNNING_COLOR);
        runStartMillis = System.currentTimeMillis();

        int customerCount = (Integer) customerCountSpinner.getValue();
        simulationService.start(customerCount, duration -> SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            customerCountSpinner.setEnabled(true);
            statusLabel.setText("● Finished");
            statusLabel.setForeground(IDLE_COLOR);
        }));
    }

    private void refreshStats() {
        setBarValue(washerBar, facility.getWashersInUse(), LaundryFacility.NUM_WASHERS);
        setBarValue(dryerBar, facility.getDryersInUse(), LaundryFacility.NUM_DRYERS);
        setBarValue(kioskBar, facility.getKiosksInUse(), LaundryFacility.NUM_KIOSKS);
        servedLabel.setText("Customers served: " + facility.getCustomersServed());
        avgTimeLabel.setText(String.format("Average time: %.0f ms", facility.getAverageServiceTimeMillis()));
        if (simulationService.isRunning()) {
            double elapsedSeconds = (System.currentTimeMillis() - runStartMillis) / 1000.0;
            elapsedLabel.setText(String.format("Elapsed: %.1f s", elapsedSeconds));
        }
    }

    private void setBarValue(JProgressBar bar, int value, int max) {
        bar.setValue(value);
        double load = max == 0 ? 0.0 : (double) value / max;
        Color color = load >= 0.8 ? HIGH_LOAD : load >= 0.5 ? MEDIUM_LOAD : LOW_LOAD;
        bar.setForeground(color);
    }

    private void appendLogAsync(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void display() {
        setVisible(true);
    }
}

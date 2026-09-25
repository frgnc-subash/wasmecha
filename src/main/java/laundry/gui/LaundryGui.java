package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import laundry.model.LaundryFacility;
import laundry.model.MachinePool;
import laundry.model.Scenario;
import laundry.service.SimulationService;
import org.springframework.stereotype.Component;

/**
 * Swing window that shows the laundromat live.
 *
 * Swing is single-threaded: every widget is created and changed on the Event
 * Dispatch Thread (EDT). Simulation threads never touch widgets. A Swing
 * Timer polls the shared facility every 200 ms, and log lines are handed
 * over with SwingUtilities.invokeLater.
 */
@Component
public class LaundryGui {

    private static final Color FREE = new Color(0xE0E0E0);
    private static final Color BUSY = new Color(0x81C784);
    private static final Color FAILED = new Color(0xE57373);
    private static final Color OUT_OF_ORDER = new Color(0x424242);

    private final SimulationService simulation;

    private JComboBox<Scenario> scenarioBox;
    private JButton startButton;
    private JLabel statusLabel;
    private JLabel washerHeader;
    private JLabel dryerHeader;
    private JLabel kioskHeader;
    private JLabel[] washerTiles;
    private JLabel[] dryerTiles;
    private JLabel[] kioskTiles;
    private JLabel statsLabel;
    private JTextArea logArea;
    private boolean hasRun; // elapsed time stays 0 until the first run

    public LaundryGui(SimulationService simulation) {
        this.simulation = simulation;
    }

    /** Builds and shows the window. Must be called on the EDT. */
    public void display() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(buildControls());
        top.add(buildShopFloor());
        top.add(buildStats());

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(top, BorderLayout.NORTH);
        content.add(buildLog(), BorderLayout.CENTER);

        JFrame frame = new JFrame("Smart Laundry Facility Simulation");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(content);
        frame.setSize(1000, 780);
        frame.setLocationRelativeTo(null);

        // Log lines come from simulation threads -> hand them to the EDT.
        simulation.addLogListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));
        new Timer(200, e -> refresh()).start();
        refresh();

        frame.setVisible(true);
    }

    // ----- Layout -----

    private JPanel buildControls() {
        scenarioBox = new JComboBox<>(Scenario.values());
        startButton = new JButton("Start simulation");
        startButton.addActionListener(e -> startSimulation());
        statusLabel = new JLabel("Idle");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.add(new JLabel("Scenario:"));
        panel.add(scenarioBox);
        panel.add(startButton);
        panel.add(statusLabel);
        return panel;
    }

    private JPanel buildShopFloor() {
        washerHeader = new JLabel();
        dryerHeader = new JLabel();
        kioskHeader = new JLabel();
        washerTiles = createTiles(LaundryFacility.NUM_WASHERS);
        dryerTiles = createTiles(LaundryFacility.NUM_DRYERS);
        kioskTiles = createTiles(LaundryFacility.NUM_KIOSKS);

        JPanel floor = new JPanel();
        floor.setLayout(new BoxLayout(floor, BoxLayout.Y_AXIS));
        floor.setBorder(BorderFactory.createTitledBorder("Shop floor"));
        floor.add(section(washerHeader, washerTiles));
        floor.add(section(dryerHeader, dryerTiles));
        floor.add(section(kioskHeader, kioskTiles));
        return floor;
    }

    private JPanel buildStats() {
        statsLabel = new JLabel();
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        panel.add(statsLabel, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane buildLog() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Event log"));
        return scroll;
    }

    private JPanel section(JLabel header, JLabel[] tiles) {
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        for (JLabel tile : tiles) {
            row.add(tile);
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        panel.add(header, BorderLayout.NORTH);
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JLabel[] createTiles(int count) {
        JLabel[] tiles = new JLabel[count];
        for (int i = 0; i < count; i++) {
            JLabel tile = new JLabel("", SwingConstants.CENTER);
            tile.setOpaque(true);
            tile.setPreferredSize(new Dimension(135, 50));
            tile.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            tiles[i] = tile;
        }
        return tiles;
    }

    // ----- Behaviour -----

    private void startSimulation() {
        logArea.setText("");
        hasRun = true;
        scenarioBox.setEnabled(false);
        startButton.setEnabled(false);
        statusLabel.setText("Running...");
        Scenario scenario = (Scenario) scenarioBox.getSelectedItem();
        simulation.start(scenario, () -> SwingUtilities.invokeLater(() -> {
            scenarioBox.setEnabled(true);
            startButton.setEnabled(true);
            statusLabel.setText("Finished");
        }));
    }

    /** Called by the Swing Timer (on the EDT) to redraw from the shared state. */
    private void refresh() {
        LaundryFacility shop = simulation.getFacility();
        paintTiles(washerTiles, "Washer", shop.washers(), false);
        paintTiles(dryerTiles, "Dryer", shop.dryers(), false);
        paintTiles(kioskTiles, "Kiosk", shop.kiosks(), shop.areKiosksDown());

        washerHeader.setText("Washers  -  waiting: " + shop.washers().getWaiting());
        dryerHeader.setText("Dryers  -  waiting: " + shop.dryers().getWaiting());
        kioskHeader.setText("Payment kiosks  -  queue: " + shop.getPaymentQueue()
            + "      Owner: " + shop.getOwnerStatus());

        statsLabel.setText(String.format(
            "<html>Arrived %d / %d &nbsp;&nbsp; Served %d &nbsp;&nbsp; Avg time %.1f s"
                + " &nbsp;&nbsp; Elapsed %.0f s<br>"
                + "Max washers in use %d / %d &nbsp;&nbsp; Max dryers in use %d / %d"
                + " &nbsp;&nbsp; Failures: washer %d, kiosk %d</html>",
            shop.getArrived(), SimulationService.NUM_CUSTOMERS, shop.getServed(),
            shop.getAverageTimeSeconds(), hasRun ? shop.elapsedMillis() / 1000.0 : 0.0,
            shop.washers().getMaxInUse(), LaundryFacility.NUM_WASHERS,
            shop.dryers().getMaxInUse(), LaundryFacility.NUM_DRYERS,
            shop.getWasherFailures(), shop.getKioskFailures()));
    }

    private void paintTiles(JLabel[] tiles, String name, MachinePool pool, boolean outOfOrder) {
        for (int i = 0; i < tiles.length; i++) {
            int customer = pool.occupantOf(i);
            String state;
            Color color;
            if (outOfOrder) {
                state = "OUT OF ORDER";
                color = OUT_OF_ORDER;
            } else if (customer == 0) {
                state = "free";
                color = FREE;
            } else if (pool.isBroken(i)) {
                state = "Customer " + customer + " - FAILED";
                color = FAILED;
            } else {
                state = "Customer " + customer;
                color = BUSY;
            }
            tiles[i].setText("<html><center><b>" + name + " " + (i + 1) + "</b><br>" + state + "</center></html>");
            tiles[i].setBackground(color);
            tiles[i].setForeground(outOfOrder ? Color.WHITE : Color.BLACK);
        }
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}

package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import laundry.model.LaundryFacility;
import laundry.model.Scenario;
import laundry.service.SimulationService;
import org.springframework.stereotype.Component;

/**
 * Main window. Assembles the components and connects them to the simulation.
 *
 * Swing is single-threaded: every widget is created and changed on the Event
 * Dispatch Thread (EDT). Simulation threads never touch widgets. A Swing
 * Timer polls the shared facility every 200 ms, and log lines are handed
 * over with SwingUtilities.invokeLater.
 */
@Component
public class LaundryGui {

    private final SimulationService simulation;

    private ControlBar controlBar;
    private Sidebar sidebar;
    private MachineSection washers;
    private MachineSection dryers;
    private MachineSection kiosks;
    private LogPanel log;

    private boolean running;
    private boolean hasRun; // elapsed time stays 0 until the first run

    public LaundryGui(SimulationService simulation) {
        this.simulation = simulation;
    }

    /** Builds and shows the window. Must be called on the EDT. */
    public void display() {
        Theme.install();

        controlBar = new ControlBar(this::start);
        sidebar = new Sidebar();
        washers = new MachineSection("Washers", "Washer", LaundryFacility.NUM_WASHERS, Theme.WASHER);
        dryers = new MachineSection("Dryers", "Dryer", LaundryFacility.NUM_DRYERS, Theme.DRYER);
        kiosks = new MachineSection("Payment kiosks", "Kiosk", LaundryFacility.NUM_KIOSKS, Theme.KIOSK);
        log = new LogPanel();

        JPanel floor = Theme.transparent(new JPanel(new GridLayout(3, 1, 0, 18)));
        floor.add(washers);
        floor.add(dryers);
        floor.add(kiosks);

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setBackground(Theme.BG);
        main.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        main.add(floor, BorderLayout.NORTH);
        main.add(log, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(controlBar, BorderLayout.NORTH);
        root.add(sidebar, BorderLayout.WEST);
        root.add(main, BorderLayout.CENTER);

        JFrame frame = new JFrame("Wasmecha");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.setSize(1120, 740);
        frame.setMinimumSize(new Dimension(860, 600));
        frame.setLocationRelativeTo(null);

        // Log lines come from simulation threads -> hand them to the EDT.
        simulation.addLogListener(line -> SwingUtilities.invokeLater(() -> log.append(line)));
        new Timer(200, e -> refresh()).start();
        refresh();

        frame.setVisible(true);
    }

    private void start(Scenario scenario) {
        if (running) {
            return;
        }
        running = true;
        hasRun = true;
        log.clear();
        controlBar.setRunning(true);

        simulation.start(scenario, () -> SwingUtilities.invokeLater(() -> {
            running = false;
            controlBar.setRunning(false);
        }));
    }

    /** Called by the Swing Timer (on the EDT) to redraw from the shared state. */
    private void refresh() {
        LaundryFacility shop = simulation.getFacility();
        washers.update(shop.washers(), shop.washers().getWaiting() + " waiting", false);
        dryers.update(shop.dryers(), shop.dryers().getWaiting() + " waiting", false);

        String kioskInfo = shop.getPaymentQueue() + " in queue";
        if (shop.getScenario() == Scenario.CONGESTED) {
            kioskInfo += "   |   owner: " + shop.getOwnerStatus().toLowerCase();
        }
        kiosks.update(shop.kiosks(), kioskInfo, shop.areKiosksDown());

        sidebar.update(shop, hasRun);
    }
}

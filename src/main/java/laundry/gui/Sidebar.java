package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import laundry.model.LaundryFacility;
import laundry.service.SimulationService;

/** Left sidebar with the live statistics. */
class Sidebar extends JPanel {

    private final JLabel arrived = value(Theme.TEXT);
    private final JLabel inShop = value(Theme.TEXT);
    private final JLabel served = value(Theme.KIOSK);
    private final JLabel avgTime = value(Theme.TEXT);
    private final JLabel elapsed = value(Theme.TEXT);
    private final JLabel peakWashers = value(Theme.WASHER);
    private final JLabel peakDryers = value(Theme.DRYER);
    private final JLabel washerFailures = value(Theme.ALERT);
    private final JLabel kioskFailures = value(Theme.ALERT);

    Sidebar() {
        super(new BorderLayout());
        setBackground(Theme.PANEL);
        setBorder(Theme.divider(0, 0, 0, 1, 16, 16));
        setPreferredSize(new Dimension(230, 0));

        JPanel stats = Theme.transparent(new JPanel(new GridLayout(0, 1, 0, 8)));
        stats.add(heading("CUSTOMERS"));
        stats.add(row("Arrived", arrived));
        stats.add(row("In shop", inShop));
        stats.add(row("Served", served));
        stats.add(row("Avg time", avgTime));
        stats.add(row("Elapsed", elapsed));
        stats.add(new JLabel());
        stats.add(heading("MACHINES"));
        stats.add(row("Max washers in use", peakWashers));
        stats.add(row("Max dryers in use", peakDryers));
        stats.add(row("Washer failures", washerFailures));
        stats.add(row("Kiosk failures", kioskFailures));

        add(stats, BorderLayout.NORTH);
    }

    /** Refreshes the numbers from the shared facility. Call on the EDT. */
    void update(LaundryFacility shop, boolean hasRun) {
        arrived.setText(shop.getArrived() + " / " + SimulationService.NUM_CUSTOMERS);
        inShop.setText(String.valueOf(shop.getArrived() - shop.getServed()));
        served.setText(String.valueOf(shop.getServed()));
        avgTime.setText(String.format("%.1f s", shop.getAverageTimeSeconds()));
        elapsed.setText(String.format("%.0f s", hasRun ? shop.elapsedMillis() / 1000.0 : 0.0));
        peakWashers.setText(shop.washers().getMaxInUse() + " / " + LaundryFacility.NUM_WASHERS);
        peakDryers.setText(shop.dryers().getMaxInUse() + " / " + LaundryFacility.NUM_DRYERS);
        washerFailures.setText(String.valueOf(shop.getWasherFailures()));
        kioskFailures.setText(String.valueOf(shop.getKioskFailures()));
    }

    private static JLabel heading(String text) {
        return Theme.label(text, Theme.SMALL, Theme.MUTED);
    }

    private static JLabel value(Color color) {
        return Theme.label("-", Theme.HEADING, color);
    }

    private static JPanel row(String name, JLabel value) {
        JPanel row = Theme.transparent(new JPanel(new BorderLayout()));
        row.add(Theme.label(name, Theme.BODY, Theme.TEXT), BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }
}

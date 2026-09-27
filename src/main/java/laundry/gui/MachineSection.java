package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import laundry.model.MachinePool;

/**
 * One row of machines (washers, dryers or kiosks): a title, a live detail
 * text and one cell per machine. Every row uses the same 6-column grid so
 * the machines line up vertically.
 */
class MachineSection extends JPanel {

    private static final int COLUMNS = 6;

    private final String machineName;
    private final Color accent;
    private final JLabel detail = Theme.label("", Theme.BODY, Theme.MUTED);
    private final JLabel[] cells;

    MachineSection(String title, String machineName, int count, Color accent) {
        super(new BorderLayout(0, 8));
        setOpaque(false);
        this.machineName = machineName;
        this.accent = accent;

        JPanel header = Theme.transparent(new JPanel(new BorderLayout()));
        header.add(Theme.label(title, Theme.HEADING, Theme.TEXT), BorderLayout.WEST);
        header.add(detail, BorderLayout.EAST);

        JPanel grid = Theme.transparent(new JPanel(new GridLayout(1, COLUMNS, 8, 0)));
        cells = new JLabel[count];
        for (int i = 0; i < COLUMNS; i++) {
            if (i < count) {
                cells[i] = createCell();
                grid.add(cells[i]);
            } else {
                grid.add(new JLabel()); // empty slot keeps the columns aligned
            }
        }

        add(header, BorderLayout.NORTH);
        add(grid, BorderLayout.CENTER);
    }

    /** Redraws every cell from the pool's current state. Call on the EDT. */
    void update(MachinePool pool, String detailText, boolean outOfOrder) {
        detail.setText(detailText);
        for (int i = 0; i < cells.length; i++) {
            int customer = pool.occupantOf(i);
            String state;
            Color color;
            if (outOfOrder) {
                state = "out of order";
                color = Theme.ALERT;
            } else if (customer == 0) {
                state = "free";
                color = Theme.MUTED;
            } else if (pool.isBroken(i)) {
                state = "failed  #" + customer;
                color = Theme.ALERT;
            } else {
                state = "#" + customer;
                color = accent;
            }
            JLabel cell = cells[i];
            String name = machineName + " " + (i + 1);
            // A free machine shows only its name; otherwise name + state.
            cell.setText(customer == 0 && !outOfOrder ? name
                : "<html><center><font color='#8C8C8C'>" + name + "</font><br>" + state + "</center></html>");
            cell.setForeground(color);
            cell.setBackground(color == Theme.ALERT ? Theme.ALERT_BG : Theme.CELL);
            cell.setBorder(BorderFactory.createLineBorder(color == Theme.MUTED ? Theme.LINE : color));
        }
    }

    private static JLabel createCell() {
        JLabel cell = new JLabel("", SwingConstants.CENTER);
        cell.setOpaque(true);
        cell.setFont(Theme.BODY);
        cell.setPreferredSize(new Dimension(80, 54));
        return cell;
    }
}

package laundry.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import laundry.model.Scenario;

/** Top bar: app title on the left, scenario picker and Start on the right. */
class ControlBar extends JPanel {

    private final JComboBox<Scenario> scenarioBox = new JComboBox<>(Scenario.values());
    private final JButton startButton = new JButton("Start simulation");

    ControlBar(Consumer<Scenario> onStart) {
        super(new BorderLayout());
        setBackground(Theme.PANEL);
        setBorder(Theme.divider(0, 0, 1, 0, 10, 16));

        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> onStart.accept((Scenario) scenarioBox.getSelectedItem()));

        JPanel controls = Theme.transparent(new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)));
        controls.add(scenarioBox);
        controls.add(startButton);

        add(Theme.label("Wasmecha", Theme.TITLE, Theme.TEXT), BorderLayout.WEST);
        add(controls, BorderLayout.EAST);
    }

    /** Locks the controls while a simulation is running. */
    void setRunning(boolean running) {
        scenarioBox.setEnabled(!running);
        startButton.setEnabled(!running);
    }
}

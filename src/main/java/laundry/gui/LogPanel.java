package laundry.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Scrolling activity log. All methods must be called on the EDT. */
class LogPanel extends JPanel {

    private final JTextArea text = new JTextArea();

    LogPanel() {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        text.setEditable(false);
        text.setLineWrap(true); // long lines wrap instead of scrolling sideways
        text.setFont(Theme.MONO);
        text.setBackground(Theme.PANEL);
        text.setForeground(new Color(0xBDBDBD));
        text.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));

        add(Theme.label("Activity", Theme.HEADING, Theme.TEXT), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    void append(String line) {
        text.append(line + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }

    void clear() {
        text.setText("");
    }
}

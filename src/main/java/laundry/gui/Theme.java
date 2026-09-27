package laundry.gui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/** Colours, fonts and small helpers shared by the GUI components. */
final class Theme {

    static final Color BG = new Color(0x0B0B0B);
    static final Color PANEL = new Color(0x121212);
    static final Color CELL = new Color(0x181818);
    static final Color LINE = new Color(0x2A2A2A);
    static final Color TEXT = new Color(0xE8E8E8);
    static final Color MUTED = new Color(0x8C8C8C);

    // Colour only where it means something.
    static final Color WASHER = new Color(0x4FC3F7);
    static final Color DRYER = new Color(0xFFB74D);
    static final Color KIOSK = new Color(0x81C784);
    static final Color ALERT = new Color(0xEF5350);
    static final Color ALERT_BG = new Color(0x2B1416);

    static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Theme() {}

    /** Recolours Swing's built-in Metal look so every standard widget is dark. */
    static void install() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        MetalLookAndFeel.setCurrentTheme(new DarkMetalTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) {
            // keep whatever look and feel is installed
        }
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    static JPanel transparent(JPanel panel) {
        panel.setOpaque(false);
        return panel;
    }

    /** A 1px divider on the chosen sides plus inner padding. */
    static Border divider(int top, int left, int bottom, int right, int padV, int padH) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(top, left, bottom, right, LINE),
            BorderFactory.createEmptyBorder(padV, padH, padV, padH));
    }

    /** Metal theme where "black" (text) is light and "white" (fields) is dark. */
    private static class DarkMetalTheme extends DefaultMetalTheme {
        @Override
        public String getName() {
            return "Dark";
        }

        @Override
        protected ColorUIResource getPrimary1() {
            return new ColorUIResource(0x5A5A5A);
        }

        @Override
        protected ColorUIResource getPrimary2() {
            return new ColorUIResource(0x2F4F63);
        }

        @Override
        protected ColorUIResource getPrimary3() {
            return new ColorUIResource(0x3A3A3A);
        }

        @Override
        protected ColorUIResource getSecondary1() {
            return new ColorUIResource(LINE);
        }

        @Override
        protected ColorUIResource getSecondary2() {
            return new ColorUIResource(0x242424);
        }

        @Override
        protected ColorUIResource getSecondary3() {
            return new ColorUIResource(BG);
        }

        @Override
        protected ColorUIResource getBlack() {
            return new ColorUIResource(TEXT);
        }

        @Override
        protected ColorUIResource getWhite() {
            return new ColorUIResource(CELL);
        }

        // Greyed-out text (disabled button / dropdown) must stay readable.
        @Override
        public ColorUIResource getInactiveControlTextColor() {
            return new ColorUIResource(0x6A6A6A);
        }

        @Override
        public ColorUIResource getInactiveSystemTextColor() {
            return new ColorUIResource(0x6A6A6A);
        }
    }
}

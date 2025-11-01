package net.miarma.integridos.client.ui;

import javax.swing.*;
import java.awt.*;

public class AutoShrinkLabel extends JLabel {
    public AutoShrinkLabel(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        String text = getText();
        if (text == null || text.isEmpty()) {
            super.paintComponent(g);
            return;
        }

        Insets insets = getInsets();
        int availableHeight = getHeight() - insets.top - insets.bottom;
        int availableWidth = getWidth() - insets.left - insets.right;

        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        Graphics2D graphics2d = (Graphics2D) g.create();

        Font font = getFont();
        int fontSize = font.getSize();

        FontMetrics fm;
        int textWidth;
        int textHeight;

        do {
            Font testFont = font.deriveFont((float) fontSize);
            fm = graphics2d.getFontMetrics(testFont);
            textWidth = fm.stringWidth(text);
            textHeight = fm.getHeight();
            if (textWidth <= availableWidth && textHeight <= availableHeight) {
                font = testFont;
                break;
            }
            fontSize--;
        } while (fontSize > 5);

        graphics2d.setFont(font);

        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

        graphics2d.drawString(text, x, y);
        graphics2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 50);
    }
}

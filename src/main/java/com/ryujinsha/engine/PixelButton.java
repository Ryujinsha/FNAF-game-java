package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;

public class PixelButton extends JButton {
    public PixelButton(String text) {
        super(text);
        setFont(new Font("Consolas", Font.BOLD, 20));
        setForeground(Color.WHITE);
        setBackground(new Color(50, 50, 50));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        // ✨ KUNCI: Menjaga tepi kotak tetap tajam (pixelated)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        if (getModel().isPressed()) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.BLACK);
        } else if (getModel().isRollover()) {
            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(new Color(30, 30, 30));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setColor(Color.WHITE);
        }

        // Gambar border luar kotak pixel
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRect(2, 2, getWidth() - 5, getHeight() - 5);

        // Gambar Teks di tengah
        FontMetrics fm = g2d.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(getText())) / 2;
        int y = (getHeight() + fm.getAscent()) / 2 - 5;
        g2d.drawString(getText(), x, y);
    }
}
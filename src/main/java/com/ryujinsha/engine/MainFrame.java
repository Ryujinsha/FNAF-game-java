package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel containerPanel;
    private JPanel fadeOverlay; // Tirai hitam untuk efek fade
    private float fadeAlpha = 0f;
    private Timer fadeTimer;

    public MainFrame() {
        setTitle("Night Shift Survival");
        setSize(1300, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Menggunakan JLayeredPane agar tirai hitam bisa berada di atas CardLayout
        JLayeredPane masterPane = new JLayeredPane();
        
        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        containerPanel.setBounds(0, 0, 1300, 900);

        // Inisialisasi Tirai Hitam (Fade Overlay)
        fadeOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        fadeOverlay.setBounds(0, 0, 1300, 900);
        fadeOverlay.setOpaque(false);
        fadeOverlay.setVisible(false);

        // Masukkan ke Master Pane
        masterPane.add(containerPanel, JLayeredPane.DEFAULT_LAYER);
        masterPane.add(fadeOverlay, JLayeredPane.DRAG_LAYER); // Layer paling atas

        containerPanel.add(new MainMenuGUI(this), "MENU");
        add(masterPane);
    }

    public void showScreen(String screenName) {
        if (screenName.equals("CUTSCENE")) {
            containerPanel.add(new CutsceneGUI(this), "CUTSCENE");
            cardLayout.show(containerPanel, "CUTSCENE");
        } 
        else if (screenName.equals("GAME")) {
            GameGUI game = new GameGUI(this);
            containerPanel.add(game, "GAME");
            cardLayout.show(containerPanel, "GAME");
            
            // ✨ MULAI EFEK FADE IN SAAT MASUK GAME
            startFadeIn(() -> game.startGame());
        } 
        else if (screenName.equals("MENU")) {
            containerPanel.add(new MainMenuGUI(this), "MENU");
            cardLayout.show(containerPanel, "MENU");
        }
    }

    // ✨ LOGIKA ANIMASI FADE IN
    private void startFadeIn(Runnable onComplete) {
        if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();

        fadeAlpha = 1.0f; // Mulai dari hitam pekat
        fadeOverlay.setVisible(true);
        fadeOverlay.repaint();

        fadeTimer = new Timer(30, e -> {
            fadeAlpha -= 0.05f; // Kurangi kepekatan perlahan
            if (fadeAlpha <= 0) {
                fadeAlpha = 0;
                fadeOverlay.setVisible(false);
                fadeTimer.stop();
                if (onComplete != null) onComplete.run();
            }
            fadeOverlay.repaint();
        });
        fadeTimer.start();
    }
}
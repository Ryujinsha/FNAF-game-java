package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainMenuGUI extends JPanel { // ✨ 1. Berubah dari JFrame menjadi JPanel
    private MainFrame mainFrame; // ✨ 2. Menampung referensi ke window utama

    public MainMenuGUI(MainFrame mainFrame) { 
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout()); // Mengatur layout dasar panel ini

        // ✨ LAYER BACKGROUND DENGAN GAMBAR MUSUH
        JPanel bgPanel = new JPanel() {
            private Image enemyImage;
            {
                URL imgUrl = getClass().getResource("/assets/enemies/enemy_c.png");
                if (imgUrl != null) {
                    enemyImage = new ImageIcon(imgUrl).getImage();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());

                if (enemyImage != null) {
                    int imgWidth = 400; 
                    int imgHeight = 600;
                    int x = (getWidth() - imgWidth) / 2;
                    int y = (getHeight() - imgHeight) / 2;
                    g.drawImage(enemyImage, x, y, imgWidth, imgHeight, this);
                }
            }
        };
        bgPanel.setLayout(new GridBagLayout()); 

        // ✨ KOMPONEN UI MENU
        JPanel uiPanel = new JPanel();
        uiPanel.setOpaque(false);
        uiPanel.setLayout(new BoxLayout(uiPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("NIGHT SHIFT SURVIVAL");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 70));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnPlay = new JButton("START SHIFT");
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 30));
        btnPlay.setBackground(Color.DARK_GRAY);
        btnPlay.setForeground(Color.GREEN);
        btnPlay.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnPlay.setFocusPainted(false);

        JButton btnQuit = new JButton("QUIT");
        btnQuit.setFont(new Font("Consolas", Font.BOLD, 30));
        btnQuit.setBackground(Color.DARK_GRAY);
        btnQuit.setForeground(Color.WHITE);
        btnQuit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnQuit.setFocusPainted(false);

        uiPanel.add(Box.createVerticalStrut(400)); 
        uiPanel.add(titleLabel);
        uiPanel.add(Box.createVerticalStrut(50));
        uiPanel.add(btnPlay);
        uiPanel.add(Box.createVerticalStrut(20));
        uiPanel.add(btnQuit);

        bgPanel.add(uiPanel);
        add(bgPanel, BorderLayout.CENTER); // Memasukkan bgPanel ke dalam MainMenuGUI

        // ✨ 3. LOGIKA TOMBOL DIPERBARUI
        btnPlay.addActionListener(e -> {
            // Memerintahkan MainFrame untuk mengganti 'kartu' ke layar Cutscene
            mainFrame.showScreen("CUTSCENE");
        });

        btnQuit.addActionListener(e -> System.exit(0));
    }
}
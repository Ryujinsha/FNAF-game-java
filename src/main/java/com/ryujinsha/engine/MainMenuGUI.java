package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class MainMenuGUI extends JFrame {

    public MainMenuGUI() {
        setTitle("Night Shift Survival - Main Menu");
        setSize(1300, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Memastikan window muncul di tengah layar

        // ✨ LAYER BACKGROUND DENGAN GAMBAR MUSUH
        JPanel bgPanel = new JPanel() {
            private Image enemyImage;
            {
                // Anda bisa mengganti enemy_a.png dengan gambar musuh lain atau siluet
                URL imgUrl = getClass().getResource("/assets/enemies/enemy_a.png");
                if (imgUrl != null) {
                    enemyImage = new ImageIcon(imgUrl).getImage();
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Latar belakang hitam pekat
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());

                // Menggambar musuh di tengah dengan ukuran yang disesuaikan
                if (enemyImage != null) {
                    int imgWidth = 400; 
                    int imgHeight = 600;
                    int x = (getWidth() - imgWidth) / 2;
                    int y = (getHeight() - imgHeight) / 2;
                    g.drawImage(enemyImage, x, y, imgWidth, imgHeight, this);
                }
            }
        };
        bgPanel.setLayout(new GridBagLayout()); // Untuk menempatkan tombol di tengah

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

        // Memberikan jarak antar elemen
        uiPanel.add(Box.createVerticalStrut(400)); // Mendorong menu ke bawah gambar musuh
        uiPanel.add(titleLabel);
        uiPanel.add(Box.createVerticalStrut(50));
        uiPanel.add(btnPlay);
        uiPanel.add(Box.createVerticalStrut(20));
        uiPanel.add(btnQuit);

        bgPanel.add(uiPanel);
        add(bgPanel);

        // ✨ LOGIKA TOMBOL
        btnPlay.addActionListener(e -> {
            this.dispose(); // Menutup window Main Menu secara permanen dari memori
            
            // Membuka GameGUI baru
            SwingUtilities.invokeLater(() -> {
                GameGUI game = new GameGUI();
                game.setLocationRelativeTo(null);
                game.startGame();
            });
        });

        btnQuit.addActionListener(e -> System.exit(0));
    }
}
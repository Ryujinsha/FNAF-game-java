package com.ryujinsha.engine;

import com.ryujinsha.system.AssetCache;
import com.ryujinsha.system.ResourceManaged;

import javax.swing.*;
import java.awt.*;

public class EndingGUI extends JPanel implements ResourceManaged {
    private MainFrame mainFrame;
    private JLayeredPane layeredPane;
    
    // Posisi awal karakter (Lari dari kiri luar layar)
    private int playerX = -100;
    private int playerY = 380; // Disesuaikan agar kaki menapak di jalan
    private Timer animationTimer;
    private int animPhase = 0;
    
    // ✨ ASET GAMBAR & LOGIKA ANIMASI
    private Image[] runFrames; // Array untuk menampung frame lari
    private int currentFrame = 0;
    private int frameDelayCounter = 0; // Mengatur kecepatan pergantian frame
    private static final int FRAME_SPEED = 5; // Semakin kecil semakin cepat lari

    private JLabel textLabel;
    private PixelButton btnRetry;
    private PixelButton btnMenu;

    public EndingGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        loadAssets(); // ✨ Muat gambar dulu
        
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        // Panel Animasi (Render Gambar)
        JPanel animPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                // Mencegah blur saat scaling
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                // Gambar Latar (Malam / Jalanan)
                g2d.setColor(new Color(15, 15, 25));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Gambar Jalan / Tanah
                g2d.setColor(Color.DARK_GRAY);
                g2d.fillRect(0, 450, getWidth(), getHeight() - 450);

                // ✨ GAMBAR KARAKTER BERLARI (Aset Gambar)
                if ((animPhase == 0 || animPhase == 1) && runFrames != null && runFrames.length > 0) {
                    // Gambar frame saat ini dengan ukuran dperbesar (misal 96x96 agar terlihat)
                    g2d.drawImage(runFrames[currentFrame], playerX, playerY, 96, 96, this);
                }
            }
        };
        animPanel.setBounds(0, 0, 1300, 900);
        layeredPane.add(animPanel, JLayeredPane.DEFAULT_LAYER);

        // Setup Teks & Tombol (Sama seperti sebelumnya)
        setupVictoryUI();

        // Timer Animasi Lari & Pergantian Frame
        animationTimer = new Timer(20, e -> {
            if (animPhase == 0) {
                playerX += 10; // Kecepatan gerak horizontal dpercepat sedikit

                // ✨ LOGIKA ANIMASI FRAME
                frameDelayCounter++;
                if (frameDelayCounter >= FRAME_SPEED) {
                    frameDelayCounter = 0;
                    currentFrame = (currentFrame + 1) % runFrames.length; // Ganti ke frame berikutnya
                }

                if (playerX > 1400) { // Karakter keluar layar
                    animPhase = 1;
                }
            } else if (animPhase == 1) {
                textLabel.setVisible(true);
                btnRetry.setVisible(true);
                btnMenu.setVisible(true);
                animationTimer.stop();
            }
            animPanel.repaint();
        });

        com.ryujinsha.system.AudioManager.stopAllSounds();
        // com.ryujinsha.system.AudioManager.playSound("/assets/audio/music/victory_theme.wav"); // Putar musik menang jika ada

        animationTimer.start();
    }

    // ✨ METHOD BARU: Memuat Aset Char_1
    private void loadAssets() {
        runFrames = new Image[4];
        runFrames[0] = AssetCache.get("/assets/cutscenes/char_1_right.png");
        runFrames[1] = AssetCache.get("/assets/cutscenes/char_1_right.png");
        runFrames[2] = AssetCache.get("/assets/cutscenes/char_1_right.png");
        runFrames[3] = AssetCache.get("/assets/cutscenes/char_1_right.png");
        
        // Catatan: Jika ada aset run_1.png dsb, ganti di atas.

        // Validasi jika aset tidak ditemukan
        for (Image img : runFrames) {
            if (img == null) {
                System.err.println("❌ [ERROR] Salah satu aset char_1_right tidak ditemukan di folder assets/cutscenes/");
            }
        }
    }

    @Override
    public void stopAllProcesses() {
        if (animationTimer != null && animationTimer.isRunning()) animationTimer.stop();
        System.out.println("[CLEANUP] Ending processes stopped.");
    }

    private void setupVictoryUI() {
        textLabel = new JLabel("KAMU BEBAS.", SwingConstants.CENTER);
        textLabel.setFont(new Font("Consolas", Font.BOLD, 70)); // Font diperbesar
        textLabel.setForeground(Color.WHITE);
        textLabel.setBounds(0, 200, 1300, 100);
        textLabel.setVisible(false);
        layeredPane.add(textLabel, JLayeredPane.PALETTE_LAYER);

        btnRetry = new PixelButton("RETRY SHIFT");
        btnRetry.setBounds(400, 600, 200, 60);
        btnRetry.setVisible(false);
        btnRetry.addActionListener(e -> mainFrame.showScreen("GAME"));

        btnMenu = new PixelButton("MAIN MENU");
        btnMenu.setBounds(700, 600, 200, 60);
        btnMenu.setVisible(false);
        btnMenu.addActionListener(e -> mainFrame.showScreen("MENU"));

        layeredPane.add(btnRetry, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(btnMenu, JLayeredPane.PALETTE_LAYER);
    }
}
package com.ryujinsha.engine;

import com.ryujinsha.database.DatabaseManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * ✨ MAIN FRAME — Jendela Utama Game (Fixed)
 *
 * Perbaikan dari versi sebelumnya:
 * - Gunakan BorderLayout pada masterPane agar containerPanel mendapat ukuran
 *   secara otomatis (tidak perlu ComponentListener manual lagi).
 * - GlassPane digunakan untuk fade overlay agar selalu di atas segalanya
 *   tanpa perlu setBounds manual.
 * - setSize(1300, 900) dikembalikan sebagai fallback sebelum maximize,
 *   agar layout benar saat window pertama kali di-pack/resize.
 * - setExtendedState dipanggil setelah add(masterPane) agar layout sudah siap.
 */
public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel containerPanel;
    private JPanel fadeOverlay;
    private float fadeAlpha = 0f;
    private Timer fadeTimer;

    // ✨ BARU: Global Developer Mode flag
    public static boolean isDevMode = false;

    private void preloadAssets() {
        // ✨ PRELOAD: Muat aset penting di awal agar gameplay lancar
        com.ryujinsha.system.AssetCache.preload(
            "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png",
            "/assets/audio/sfx/button_click.wav",
            "/assets/audio/sfx/jumpscare_scream.wav"
        );
    }

    public MainFrame() {
        setTitle("The Last Door");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 450));
        setSize(1300, 900); // ✨ Fallback size agar layout punya ukuran awal
        setLocationRelativeTo(null);
        setResizable(true);

        // ============================================================
        // SETUP CONTENT PANE
        // ============================================================

        // containerPanel sebagai card holder — mengisi seluruh frame
        cardLayout    = new CardLayout();
        containerPanel = new JPanel(cardLayout);
        containerPanel.setBackground(Color.BLACK);

        // Frame pakai BorderLayout default — containerPanel langsung jadi CENTER
        setContentPane(containerPanel);

        // ============================================================
        // FADE OVERLAY via GlassPane
        // ============================================================

        // GlassPane selalu menutupi seluruh frame — sempurna untuk efek fade
        fadeOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Tidak perlu super.paintComponent agar benar-benar transparan
                Graphics2D g2d = (Graphics2D) g;
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        fadeOverlay.setOpaque(false);
        fadeOverlay.setVisible(false);
        // GlassPane secara default menutupi seluruh JFrame dan menerima events
        setGlassPane(fadeOverlay);

        preloadAssets();

        // ✨ LEADERBOARD: Inisialisasi koneksi database
        DatabaseManager.initialize();

        // ============================================================
        // TAMBAH SCREEN AWAL
        // ============================================================
        containerPanel.add(new MainMenuGUI(this), "MENU");

        // ============================================================
        // FORCE FULLSCREEN — dipanggil setelah setup selesai
        // ============================================================
        setUndecorated(true);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    // ============================================================
    // NAVIGASI LAYAR
    // ============================================================

    public void showScreen(String screenName) {
        // ✨ CLEANUP: Hentikan semua proses latar belakang di layar sebelumnya
        cleanupCurrentScreen();

        switch (screenName) {
            case "CUTSCENE":
                containerPanel.add(new CutsceneGUI(this), "CUTSCENE");
                cardLayout.show(containerPanel, "CUTSCENE");
                break;

            case "GAME":
                GameGUI game = new GameGUI(this);
                containerPanel.add(game, "GAME");
                cardLayout.show(containerPanel, "GAME");
                startFadeIn(() -> game.startGame());
                break;

            case "MENU":
                containerPanel.add(new MainMenuGUI(this), "MENU");
                cardLayout.show(containerPanel, "MENU");
                break;

            case "ENDING":
                containerPanel.add(new EndingGUI(this), "ENDING");
                cardLayout.show(containerPanel, "ENDING");
                startFadeIn(null);
                break;

            case "LEADERBOARD":
                containerPanel.add(new LeaderboardGUI(this), "LEADERBOARD");
                cardLayout.show(containerPanel, "LEADERBOARD");
                break;

            default:
                System.err.println("[MainFrame] Screen tidak dikenal: " + screenName);
        }
    }

    /** Mencari panel aktif dan menghentikan prosesnya jika mengimplementasikan ResourceManaged */
    private void cleanupCurrentScreen() {
        for (Component comp : containerPanel.getComponents()) {
            if (comp.isVisible() && comp instanceof com.ryujinsha.system.ResourceManaged) {
                ((com.ryujinsha.system.ResourceManaged) comp).stopAllProcesses();
            }
        }
        // Hapus semua komponen lama untuk membebaskan memory (karena kita selalu buat instance baru di showScreen)
        containerPanel.removeAll();
        containerPanel.revalidate();
    }

    // ============================================================
    // ANIMASI FADE
    // ============================================================

    /** Fade in: dari hitam pekat → transparan */
    private void startFadeIn(Runnable onComplete) {
        if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();

        fadeAlpha = 1.0f;
        fadeOverlay.setVisible(true);
        fadeOverlay.repaint();

        fadeTimer = new Timer(30, e -> {
            fadeAlpha -= 0.05f;
            if (fadeAlpha <= 0f) {
                fadeAlpha = 0f;
                fadeOverlay.setVisible(false);
                fadeTimer.stop();
                if (onComplete != null) onComplete.run();
            }
            fadeOverlay.repaint();
        });
        fadeTimer.start();
    }

    /** Fade out: dari transparan → hitam pekat, lalu pindah screen */
    public void fadeOutToScreen(String screenName) {
        if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();

        fadeOverlay.setVisible(true);
        fadeAlpha = 0f;

        fadeTimer = new Timer(40, e -> {
            fadeAlpha += 0.05f;
            if (fadeAlpha >= 1.0f) {
                fadeAlpha = 1.0f;
                fadeTimer.stop();
                showScreen(screenName);
            }
            fadeOverlay.repaint();
        });
        fadeTimer.start();
    }
}
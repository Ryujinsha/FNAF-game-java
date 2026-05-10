package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AssetCache;
import com.ryujinsha.system.AudioManager;
import com.ryujinsha.system.ResourceManaged;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * ✨ GameGUI — Layar Permainan Utama (Versi Refactored)
 *
 * Perubahan dari versi sebelumnya:
 * - Hitbox terpusat di HitboxConfig (mudah dikalibrasi)
 * - Rendering terpusat di RenderEngine (satu sumber kebenaran)
 * - Gambar di-cache via AssetCache (tidak re-load berulang)
 * - MainFrame responsif via ComponentListener
 * - Bug path idle Hina diperbaiki
 * - Konsistensi EnemyB sebagai entitas ventilasi
 */
public class GameGUI extends JPanel implements ResourceManaged {

    private MainFrame mainFrame;

    // ============================================================
    // 1. DATA LOGIC GAME
    // ============================================================
    private Player player;
    private EnemyOdd enemyA; // The Red One (Pintu Depan)
    private EnemyEven enemyB; // Hina (Ventilasi)
    private boolean areEnemiesActive = true;

    // ✨ State Management Enums
    private GameState currentState = GameState.PLAYING;
    private PlayerPosition currentPosition = PlayerPosition.FRONT_ROOM;

    private JLabel statusLabel;
    private PixelButton btnDoor, btnLookLeft, btnLookRight;
    private JLayeredPane layeredPane;
    private JPanel officePanel;

    // ============================================================
    // 2. SISTEM LOCKPICK
    // ============================================================
    private JPanel lockpickPopupPanel;
    private int lockBars = 0;
    private Timer lockDrainTimer;

    // QTE Timing Hit fields
    private double qteIndicatorPos = 0.0;   // Posisi indikator (0.0 - 1.0)
    private int qteDirection = 1;            // Arah gerak (+1 kanan, -1 kiri)
    private Timer qteAnimTimer;              // Timer untuk animasi indikator
    private boolean qteActive = false;       // Apakah QTE sedang aktif
    private JPanel qteRenderPanel;           // Panel rendering QTE

    // ============================================================
    // 3. MEKANIK BERSEMBUNYI, STRUGGLE & ANIMASI
    // ============================================================
    // (currentPosition == PlayerPosition.CABINET) dan (currentState == GameState.STRUGGLING) sekarang menggunakan currentState dan currentPosition
    private int struggleValue = 50;
    private Timer struggleTimer;
    private Enemy currentAttacker = null;
    private boolean hidEarly = false;

    // Aset Animasi QTE Kabinet
    private Image qteBodyImg, qteHandLeftImg, qteHandRightImg;
    private int struggleAnimCounter = 0;

    // ✨ BARU: Sistem Animasi Retreat (Mundur)
    private boolean isRetreating = false;
    private int retreatAnimTicks = 0;
    private Enemy lastDefeatedEnemy = null;
    private Image retreatImg = null;
    private Timer retreatTimer;

    // ============================================================
    // 4. UI OVERLAY
    // ============================================================
    private JPanel endScreenPanel;
    private JLabel endTitleLabel;
    private JLabel endMessageLabel;

    // ============================================================
    // 5. VISUAL STATE
    // ============================================================
    private Image doorEnemyVisual = null;
    private Image ventEnemyVisual = null;

    private Timer gameLoopTimer;
    private Timer quoteTimer;
    // (currentPosition == PlayerPosition.BACK_ROOM) sekarang menggunakan currentPosition
    private float vignetteIntensity = 0f; // ✨ BARU: Intensitas efek vignette
    private boolean isFlickering = false; // ✨ BARU: Status lampu mati-nyala
    private float flickerAlpha = 0f;      // ✨ BARU: Opacity overlay hitam saat flicker
    private Timer flickerTimer;           // ✨ BARU: Timer untuk kontrol flicker
    private boolean hasFlickeredForPhase2 = false; // ✨ BARU: Flag agar flicker hanya 1x per approach
    private boolean hasFlickeredForEnemyA = false; // ✨ BARU: Flag agar flicker hanya 1x per approach (The Red One)
    private boolean hasFlickeredForDanger = false; // ✨ BARU: Flag untuk flicker peringatan terakhir (patience 1)
    private java.util.List<String> devLogs = new java.util.ArrayList<>(); // ✨ BARU: Cache log untuk Dev Mode
    
    // ✨ BARU: Peek & Flashlight mechanics
    private boolean isFlashlightOn = false;

    // ✨ BARU: Incoming Stage fields
    private boolean incomingDialogVisible = false;
    private Timer incomingTimer;

    // ✨ BARU: Hallway Stage fields
    private boolean hasHallwayKey = false;
    private boolean hallwayCutsceneActive = false;
    private int hallwayCutsceneIndex = 0;
    private String[] hallwayCutsceneTexts = {
        "Kamu merasa sendirian. Namun, tidak ada jalan kembali",
        "Kamu berlari sampai kau menuju sebuah ruangan kecil. Kamu mendengar suara serangga dan kamu berasumsi kamu akan bebas",
        "Namun, tidak semudah itu. Mereka marah dan ingin menyerangmu"
    };
    private String currentDisplayedText = "";
    private Timer typingTimer;
    private int typingCharIndex = 0;

    // Path konstanta aset ruangan
    private static final String PATH_FRONT_ROOM = "/assets/rooms/front_room.png";
    private static final String PATH_FRONT_DOOR = "/assets/rooms/front_door.png";
    private static final String PATH_BACK_DOOR = "/assets/rooms/back_door.png";
    private static final String PATH_BACK_DOOR_OPENED = "/assets/rooms/back_door_opened.png";
    private static final String PATH_LOCK_DOOR = "/assets/rooms/lock_door.png";
    private static final String PATH_HALLWAY = "/assets/rooms/hallway.png";

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public GameGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initGameData();
        preloadCriticalAssets();

        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        layeredPane = new JLayeredPane();
        layeredPane.setBackground(Color.BLACK);
        layeredPane.setOpaque(true);
        add(layeredPane, BorderLayout.CENTER);

        setupUI();
        setupResponsiveListener();
        setupGameLoop();
        setupKeyBindings();
    }

    // ============================================================
    // INISIALISASI DATA
    // ============================================================

    private void initGameData() {
        this.player = new Player("Night Guard");
        this.enemyA = new EnemyOdd("The Red One", 20);
        this.enemyB = new EnemyEven("Hina", 20);

        this.areEnemiesActive = false; // ✨ Default false untuk babak INCOMING
        this.currentState = GameState.HALLWAY;
        this.currentPosition = PlayerPosition.FRONT_ROOM;
        this.incomingDialogVisible = true;
        this.hasHallwayKey = false;
        this.hallwayCutsceneActive = false;
        this.hallwayCutsceneIndex = 0;
        this.lockBars = 0;
        this.qteIndicatorPos = 0.0;
        this.qteDirection = 1;
        this.qteActive = false;
        this.struggleValue = 50;
        this.doorEnemyVisual = null;
        this.ventEnemyVisual = null;
        this.hidEarly = false;
        this.struggleAnimCounter = 0;
        this.isFlickering = false;
        this.flickerAlpha = 0f;
        this.hasFlickeredForPhase2 = false;
        this.hasFlickeredForEnemyA = false;
        this.hasFlickeredForDanger = false;

        // Reset retreat state
        this.isRetreating = false;
        this.retreatAnimTicks = 0;
        this.lastDefeatedEnemy = null;
        this.retreatImg = null;
    }

    /**
     * Pre-load semua aset penting ke AssetCache sebelum game dimulai.
     * Ini mencegah lag saat runtime pertama kali aset dibutuhkan.
     */
    private void preloadCriticalAssets() {
        AssetCache.preload(
                PATH_FRONT_ROOM,
                PATH_FRONT_DOOR,
                PATH_BACK_DOOR,
                PATH_BACK_DOOR_OPENED,
                PATH_LOCK_DOOR,
                PATH_HALLWAY,
                "/assets/keyhole/tunnel.png",
                "/assets/keyhole/tunnel_door.png",
                "/assets/vent/vent_back.png",
                "/assets/vent/vent_front.png",
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png",
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-2.png",
                "/assets/enemies/enemy_b_vent/idle/hina_idle_phase_1.png",
                "/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
    }

    // ============================================================
    // SETUP UI
    // ============================================================

    private void setupUI() {
        // Status bar atas
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.DARK_GRAY);
        statusLabel = new JLabel(
                "Objective: Bobol gembok pintu belakang.  [A/D]=Lihat  [W]=Sembunyi  [S]=Gembok  [SPASI]=Hit  [ESC]=Keluar");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        // Panel render utama (menggambar background + enemy)
        // ✨ FIX: Mouse listener dipasang di officePanel (bukan layeredPane) karena
        // officePanel menutupi seluruh layeredPane sehingga semua klik masuk ke sini.
        officePanel = new GameRenderer(this);
        layeredPane.add(officePanel, JLayeredPane.DEFAULT_LAYER);

        setupInteractionHits();
        setupLockpickUI();
        layeredPane.add(lockpickPopupPanel, JLayeredPane.MODAL_LAYER);
        lockpickPopupPanel.setVisible(false);

        setupEndScreen();
        layeredPane.add(endScreenPanel, JLayeredPane.POPUP_LAYER);
        endScreenPanel.setVisible(false);

        setupFloatingControls();
    }

    // ============================================================
    // RENDERING UTAMA DILAKUKAN OLEH GameRenderer
    // ============================================================



    // ============================================================
    // SETUP INTERAKSI KLIK (HITBOX SYSTEM)
    // ============================================================

    private void setupInteractionHits() {
        // ✨ FIX KRITIS: Listener dipasang di officePanel, BUKAN layeredPane.
        // layeredPane tidak pernah menerima klik karena officePanel menutupi
        // seluruh areanya. Swing mengirim event ke child paling atas (officePanel).
        officePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if ((currentState == GameState.GAMEOVER))
                    return;
                requestFocusInWindow();

                // Prioritas 1: Struggle QTE - klik mana pun dihitung
                if ((currentState == GameState.STRUGGLING)) {
                    struggleValue += 15;
                    if (struggleValue >= 100)
                        checkStruggleWin();
                    else {
                        struggleAnimCounter += 2;
                        officePanel.repaint();
                    }
                    return;
                }

                // Prioritas 2: Keluar dari kabinet
                if ((currentPosition == PlayerPosition.CABINET)) {
                    exitCabinet();
                    return;
                }

                // ✨ Konversi klik layar → koordinat game space via RenderEngine
                Rectangle bounds = RenderEngine.getGameBounds(
                        officePanel.getWidth(), officePanel.getHeight());
                Point gamePoint = RenderEngine.screenToGame(e.getPoint(), bounds);

                // Abaikan klik di black bar (luar area game)
                if (gamePoint.x < 0)
                    return;

                // Prioritas 2.1: Klik di Hallway
                if (currentState == GameState.HALLWAY) {
                    if (incomingDialogVisible) {
                        incomingDialogVisible = false;
                        officePanel.repaint();
                        return;
                    }
                    if (hallwayCutsceneActive) {
                        advanceHallwayCutscene();
                    } else {
                        handleHallwayClick(gamePoint);
                    }
                    return;
                }

                // Prioritas 3: Interaksi di back room
            }
        });
    }

    /**
     * Menangani klik di ruang belakang.
     * Hitbox diambil dari HitboxConfig — mudah di-tweak tanpa modifikasi logika.
     */
    private void handleBackRoomClick(Point gamePoint) {
        // ✨ Hitbox Kabinet (kanan bawah back room) — dari HitboxConfig
        if (RenderEngine.hitboxContains(HitboxConfig.CABINET_HITBOX, gamePoint)) {
            enterCabinet();
            return;
        }

        // ✨ Hitbox Pintu Gembok (tengah-kiri back room) — dari HitboxConfig
        if (RenderEngine.hitboxContains(HitboxConfig.LOCKDOOR_HITBOX, gamePoint)) {
            logEvent("🔧 [INTERACT] Mendekat ke gembok untuk mencongkel...");
            lockpickPopupPanel.setVisible(true);
            startQte();
            updateUIVisibility();
        }
    }

    private void enterCabinet() {
        currentPosition = PlayerPosition.CABINET;

        // Tentukan apakah bersembunyi "lebih awal" (aman) atau telat (QTE)
        Enemy attacker = getEnemyAtDoor();
        if (attacker != null) {
            hidEarly = (attacker.getPatienceTimer() >= 3);
        } else {
            hidEarly = true; // Tidak ada musuh = aman
        }

        AudioManager.playSound("/assets/audio/sfx/door_close.wav");
        logEvent("🚪 [HIDE] Kamu meringkuk masuk ke kabinet.");
        updateUIVisibility();
        officePanel.repaint();
    }

    private void exitCabinet() {
        currentPosition = PlayerPosition.BACK_ROOM;
        AudioManager.playSound("/assets/audio/sfx/door_open.wav");
        logEvent("🚪 Kamu merangkak keluar dari kabinet.");
        updateUIVisibility();
        officePanel.repaint();
    }

    // ============================================================
    // SETUP LOCKPICK UI
    // ============================================================

    private void setupLockpickUI() {
        lockpickPopupPanel = new JPanel();
        lockpickPopupPanel.setBackground(Color.BLACK);
        lockpickPopupPanel.setLayout(new BorderLayout());

        // ✨ QTE TIMING HIT PANEL
        qteRenderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Latar belakang lockpick (menggunakan cangkang)
                Rectangle bounds = RenderEngine.getGameBounds(getWidth(), getHeight());
                Image imgLock = AssetCache.get(PATH_LOCK_DOOR);
                if (imgLock != null) {
                    g2d.drawImage(imgLock, bounds.x, bounds.y, bounds.width, bounds.height, this);
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                int pw = getWidth();
                int ph = getHeight();

                // Judul
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Consolas", Font.BOLD, 28));
                String title = "M E N C O N G K E L   G E M B O K";
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(title, (pw - fm.stringWidth(title)) / 2, 80);

                // Instruksi
                g2d.setFont(new Font("Consolas", Font.PLAIN, 22));
                String sub = "< Tekan [SPASI] tepat di zona HIJAU! >";
                fm = g2d.getFontMetrics();
                g2d.drawString(sub, (pw - fm.stringWidth(sub)) / 2, 130);

                // ============================================================
                // QTE BAR — Bar horizontal dengan zona hijau + indikator
                // ============================================================
                int qteBarW = Math.min(700, pw - 100);
                int qteBarH = 50;
                int qteBarX = (pw - qteBarW) / 2;
                int qteBarY = ph / 2 - 60;

                // Background bar (abu gelap)
                g2d.setColor(new Color(40, 40, 40, 220));
                g2d.fillRoundRect(qteBarX, qteBarY, qteBarW, qteBarH, 12, 12);

                // Zona Hijau (tengah bar, ukuran tergantung level)
                double greenFraction = getGreenZoneWidth();
                int greenW = (int) (qteBarW * greenFraction);
                int greenX = qteBarX + (qteBarW - greenW) / 2;
                g2d.setColor(new Color(0, 200, 0, 160));
                g2d.fillRoundRect(greenX, qteBarY + 2, greenW, qteBarH - 4, 8, 8);

                // Border zona hijau
                g2d.setColor(new Color(0, 255, 0, 220));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(greenX, qteBarY + 2, greenW, qteBarH - 4, 8, 8);

                // Border bar luar
                g2d.setColor(new Color(100, 100, 100));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(qteBarX, qteBarY, qteBarW, qteBarH, 12, 12);

                // ✨ INDIKATOR — Segitiga / garis bergerak
                if (qteActive) {
                    int indicatorX = qteBarX + (int) (qteIndicatorPos * qteBarW);
                    int indicatorW = 6;

                    // Garis indikator
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(4));
                    g2d.drawLine(indicatorX, qteBarY - 5, indicatorX, qteBarY + qteBarH + 5);

                    // Segitiga atas (panah ke bawah)
                    int[] triX = {indicatorX - 10, indicatorX + 10, indicatorX};
                    int[] triY = {qteBarY - 20, qteBarY - 20, qteBarY - 5};
                    g2d.setColor(Color.YELLOW);
                    g2d.fillPolygon(triX, triY, 3);

                    // Segitiga bawah (panah ke atas)
                    int[] triX2 = {indicatorX - 10, indicatorX + 10, indicatorX};
                    int[] triY2 = {qteBarY + qteBarH + 20, qteBarY + qteBarH + 20, qteBarY + qteBarH + 5};
                    g2d.fillPolygon(triX2, triY2, 3);
                }

                // Level indicator text
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Consolas", Font.BOLD, 20));
                String levelText = "Level " + (lockBars + 1) + " / 6";
                fm = g2d.getFontMetrics();
                g2d.drawString(levelText, (pw - fm.stringWidth(levelText)) / 2, qteBarY - 35);

                // ============================================================
                // 6 BAR PROGRESS — Di bawah QTE bar
                // ============================================================
                int barW = 80, barSpacing = 20;
                int startX = (pw - (6 * barW + 5 * barSpacing)) / 2;
                int barY = qteBarY + qteBarH + 80;
                for (int i = 0; i < 6; i++) {
                    boolean filled = (i < lockBars);
                    // Gradient fill untuk bar terisi
                    if (filled) {
                        g2d.setColor(new Color(0, 220, 0, 200));
                    } else if (i == lockBars) {
                        // Bar aktif saat ini — highlight
                        g2d.setColor(new Color(80, 80, 0, 200));
                    } else {
                        g2d.setColor(new Color(50, 50, 50, 200));
                    }
                    g2d.fillRoundRect(startX + i * (barW + barSpacing), barY, barW, 50, 8, 8);

                    // Border
                    if (filled) {
                        g2d.setColor(new Color(100, 255, 100));
                    } else if (i == lockBars) {
                        g2d.setColor(Color.YELLOW);
                    } else {
                        g2d.setColor(Color.GRAY);
                    }
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRoundRect(startX + i * (barW + barSpacing), barY, barW, 50, 8, 8);

                    // Nomor bar
                    g2d.setFont(new Font("Consolas", Font.BOLD, 18));
                    fm = g2d.getFontMetrics();
                    String num = String.valueOf(i + 1);
                    int textX = startX + i * (barW + barSpacing) + (barW - fm.stringWidth(num)) / 2;
                    int textY = barY + 32;
                    g2d.setColor(filled ? Color.WHITE : Color.GRAY);
                    g2d.drawString(num, textX, textY);
                }
            }
        };
        lockpickPopupPanel.add(qteRenderPanel, BorderLayout.CENTER);

        JButton btnClose = new JButton("MENJAUH DARI PINTU [S / ESC]");
        btnClose.setBackground(Color.DARK_GRAY);
        btnClose.setForeground(Color.WHITE);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 20));
        btnClose.setPreferredSize(new Dimension(getWidth(), 60));
        btnClose.addActionListener(e -> {
            stopQte();
            lockpickPopupPanel.setVisible(false);
            updateUIVisibility();
        });
        lockpickPopupPanel.add(btnClose, BorderLayout.SOUTH);
    }

    // ============================================================
    // QTE TIMING HIT SYSTEM
    // ============================================================

    /** Lebar zona hijau berdasarkan level saat ini (lockBars). Semakin tinggi semakin kecil. */
    private double getGreenZoneWidth() {
        // Bar 0 = 35%, Bar 1 = 31%, ... Bar 5 = 15%
        return 0.35 - (lockBars * 0.04);
    }

    /** Kecepatan indikator berdasarkan level saat ini. Semakin tinggi semakin cepat. */
    private double getQteSpeed() {
        // Bar 0 = 0.015, Bar 1 = 0.019, ... Bar 5 = 0.035
        return 0.015 + (lockBars * 0.004);
    }

    /** Mulai QTE: reset indikator dan mulai timer animasi. */
    private void startQte() {
        if (currentState == GameState.INCOMING) {
            switchToActualGame();
        }
        
        qteIndicatorPos = 0.0;
        qteDirection = 1;
        qteActive = true;

        if (qteAnimTimer != null && qteAnimTimer.isRunning())
            qteAnimTimer.stop();

        // ~60fps animasi indikator
        qteAnimTimer = new Timer(16, e -> {
            if (!qteActive || (currentState == GameState.GAMEOVER)) {
                ((Timer) e.getSource()).stop();
                return;
            }
            qteIndicatorPos += getQteSpeed() * qteDirection;

            // Pantulkan di ujung bar
            if (qteIndicatorPos >= 1.0) {
                qteIndicatorPos = 1.0;
                qteDirection = -1;
            } else if (qteIndicatorPos <= 0.0) {
                qteIndicatorPos = 0.0;
                qteDirection = 1;
            }

            if (qteRenderPanel != null)
                qteRenderPanel.repaint();
        });
        qteAnimTimer.start();
    }

    /** Hentikan QTE: stop timer, reset state. */
    private void stopQte() {
        qteActive = false;
        if (qteAnimTimer != null && qteAnimTimer.isRunning())
            qteAnimTimer.stop();
    }

    /** Dipanggil saat player menekan SPASI di layar QTE gembok. */
    private void handleQteHit() {
        if (!qteActive || lockBars >= 6 || (currentState == GameState.GAMEOVER))
            return;

        double greenWidth = getGreenZoneWidth();
        double greenStart = 0.5 - greenWidth / 2;
        double greenEnd = 0.5 + greenWidth / 2;

        if (qteIndicatorPos >= greenStart && qteIndicatorPos <= greenEnd) {
            // ✅ HIT! Bar terisi
            lockBars++;
            logEvent("✅ [QTE HIT] Bar " + lockBars + "/6 berhasil!");
            AudioManager.playSound("/assets/audio/sfx/button_click.wav");

            if (lockBars >= 6) {
                stopQte();
                handleLockpickSuccess();
            } else {
                // Reset indikator untuk level berikutnya
                qteIndicatorPos = 0.0;
                qteDirection = 1;
            }
        } else {
            // ❌ MISS! Penalti mundur 1 bar
            lockBars = Math.max(0, lockBars - 1);
            logEvent("❌ [QTE MISS] Lockpick terpeleset! Mundur ke bar " + lockBars + "/6.");
            AudioManager.playSound("/assets/audio/sfx/button_click.wav");

            // Reset indikator
            qteIndicatorPos = 0.0;
            qteDirection = 1;
        }

        if (qteRenderPanel != null)
            qteRenderPanel.repaint();
    }

    private void handleLockpickSuccess() {
        currentState = GameState.GAMEOVER;
        gameLoopTimer.stop();
        if (lockDrainTimer != null)
            lockDrainTimer.stop();

        logEvent("✅ [VICTORY] Gembok berhasil dirusak! Rantai terlepas...");
        AudioManager.playSound("/assets/audio/sfx/door_open.wav");
        officePanel.repaint();

        Timer winDelay = new Timer(2000, evt -> mainFrame.fadeOutToScreen("ENDING"));
        winDelay.setRepeats(false);
        winDelay.start();
    }

    // ============================================================
    // SISTEM ASET ENEMY
    // ============================================================

    /**
     * Load aset QTE untuk enemy tertentu ke-cache.
     * ✨ FIX: Hina sekarang menggunakan path yang benar.
     */
    private boolean loadQteAssets(Enemy enemy) {
        if (enemy == enemyA) {
            String base = "/assets/enemies/enemy_a_door/qte-state/";
            qteBodyImg = AssetCache.get(base + "the-red-one-body.png");
            qteHandLeftImg = AssetCache.get(base + "the-red-one-left-hand.png");
            qteHandRightImg = AssetCache.get(base + "the-red-one-right-hand.png");
        } else if (enemy == enemyB) {
            String base = "/assets/enemies/enemy_b_vent/qte-state/";
            qteBodyImg = AssetCache.get(base + "hina_body_qte.png");
            qteHandLeftImg = AssetCache.get(base + "hina_left_hand_qte.png");
            qteHandRightImg = AssetCache.get(base + "hina_right_hand_qte.png");
        } else {
            return false;
        }
        return (qteBodyImg != null && qteHandLeftImg != null && qteHandRightImg != null);
    }

    /**
     * Ambil sprite idle enemy.
     * ✨ FIX: Hina sekarang menggunakan path idle yang benar (bukan QTE body).
     */
    public Image getIdleSprite(Enemy enemy) {
        if (enemy == enemyA) {
            // The Red One — gunakan phase 1 untuk idle
            return AssetCache.get("/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png");
        } else if (enemy == enemyB) {
            // ✨ FIX: Path idle Hina yang benar (bukan qte-state!)
            return AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
        }
        return null;
    }

    private void updateDoorVisuals() {
        doorEnemyVisual = null;
        ventEnemyVisual = null;
        Enemy enemy = getEnemyAtDoor();

        // Enemy A: Tampilkan sprite saat sudah dekat (patience <= 3)
        // ✨ FIX: Diubah dari <= 2 ke <= 3 agar terlihat saat pintu terbuka
        if (enemy != null && enemy == enemyA && enemy.getPatienceTimer() <= 3
                && !(currentState == GameState.STRUGGLING) && !isRetreating) {
            doorEnemyVisual = getIdleSprite(enemy);
        }
        // Enemy B (Hina): All phase rendering handled in paintFrontRoom()
        officePanel.repaint();
    }

    // ============================================================
    // SETUP RESPONSIVE LAYOUT
    // ============================================================

    private void setupResponsiveListener() {
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();

                // Panel utama mengisi seluruh layered pane
                officePanel.setBounds(0, 0, w, h);
                endScreenPanel.setBounds(0, 0, w, h);
                lockpickPopupPanel.setBounds(0, 0, w, h);

                // ✨ Tombol tengah (TUTUP PINTU) — mengikuti posisi di dalam cangkang game
                int btnW = HitboxConfig.BTN_CENTER_W;
                int btnH = HitboxConfig.BTN_CENTER_H;
                int startX = (w - btnW) / 2;
                int posY = h - btnH - HitboxConfig.BTN_CENTER_MARGIN_BOTTOM;
                btnDoor.setBounds(startX, posY, btnW, btnH);

                // ✨ Tombol Look Left/Right — di tepi kiri/kanan panel
                int ew = HitboxConfig.BTN_EDGE_W;
                int eh = HitboxConfig.BTN_EDGE_H;
                int midY = (h - eh) / 2;
                btnLookLeft.setBounds(HitboxConfig.BTN_EDGE_MARGIN_SIDE, midY, ew, eh);
                btnLookRight.setBounds(w - ew - HitboxConfig.BTN_EDGE_MARGIN_SIDE, midY, ew, eh);
            }
        });
    }

    // ============================================================
    // SETUP END SCREEN
    // ============================================================

    private void setupEndScreen() {
        endScreenPanel = new JPanel();
        endScreenPanel.setBackground(new Color(0, 0, 0, 220));
        endScreenPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 20, 0);

        endTitleLabel = new JLabel("GAME OVER", SwingConstants.CENTER);
        endTitleLabel.setFont(new Font("Consolas", Font.BOLD, 60));
        endScreenPanel.add(endTitleLabel, gbc);

        endMessageLabel = new JLabel("Message", SwingConstants.CENTER);
        endMessageLabel.setFont(new Font("Consolas", Font.PLAIN, 20));
        endMessageLabel.setForeground(Color.WHITE);
        endScreenPanel.add(endMessageLabel, gbc);

        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);

        JButton btnRetry = new JButton("RETRY SHIFT");
        btnRetry.setFont(new Font("Consolas", Font.BOLD, 20));
        btnRetry.setBackground(Color.DARK_GRAY);
        btnRetry.setForeground(Color.GREEN);
        btnRetry.addActionListener(e -> resetGame());

        JButton btnMenu = new JButton("MAIN MENU");
        btnMenu.setFont(new Font("Consolas", Font.BOLD, 20));
        btnMenu.setBackground(Color.DARK_GRAY);
        btnMenu.setForeground(Color.WHITE);
        btnMenu.addActionListener(e -> {
            stopAllTimers();
            AudioManager.stopAllSounds();
            mainFrame.showScreen("MENU");
        });

        btnPanel.add(btnRetry);
        btnPanel.add(btnMenu);
        endScreenPanel.add(btnPanel, gbc);
    }

    // ============================================================
    // SETUP FLOATING CONTROLS
    // ============================================================

    private void setupFloatingControls() {
        btnDoor = new PixelButton("🚪 TUTUP PINTU DEPAN [E]");
        btnLookLeft = new PixelButton("◀ [A]");
        btnLookRight = new PixelButton("▶ [D]");

        layeredPane.add(btnDoor, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnLookLeft, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnLookRight, JLayeredPane.MODAL_LAYER);

        java.awt.event.ActionListener lookAction = e -> {
            if ((currentState == GameState.GAMEOVER) || lockpickPopupPanel.isVisible() || (currentPosition == PlayerPosition.CABINET))
                return;
            currentPosition = (currentPosition == PlayerPosition.FRONT_ROOM) ? PlayerPosition.BACK_ROOM : PlayerPosition.FRONT_ROOM;
            updateUIVisibility();
            officePanel.repaint();
        };
        btnLookLeft.addActionListener(lookAction);
        btnLookRight.addActionListener(lookAction);

        btnDoor.addActionListener(e -> {
            // ✨ MODIFIKASI: Player tidak bisa kontrol manual lagi sesuai permintaan user.
            // Biarkan saja method ini kosong atau tampilkan pesan.
            logEvent("Pintu ini sekarang hanya dikontrol oleh sistem/entitas.");
        });

        updateUIVisibility();
    }

    // ============================================================
    // SETUP KEY BINDINGS
    // ============================================================

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // F11 — Toggle fullscreen
        im.put(KeyStroke.getKeyStroke("F11"), "toggleMaximize");
        am.put("toggleMaximize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(GameGUI.this);
                if (frame != null) {
                    int state = frame.getExtendedState();
                    frame.setExtendedState(
                            state == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // SPACE — QTE Lockpick Hit + Spam Struggle
        im.put(KeyStroke.getKeyStroke("SPACE"), "spamSpace");
        am.put("spamSpace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((currentState == GameState.GAMEOVER)) return;
                // Prioritas 1: QTE Lockpick
                if (qteActive && lockpickPopupPanel.isVisible()) {
                    handleQteHit();
                    return;
                }
                // Prioritas 2: Struggle QTE
                if ((currentState == GameState.STRUGGLING)) {
                    struggleValue += 15;
                    if (struggleValue >= 100)
                        checkStruggleWin();
                    else {
                        struggleAnimCounter += 2;
                        officePanel.repaint();
                    }
                }
            }
        });

        // A — Lihat kiri / kanan
        im.put(KeyStroke.getKeyStroke("A"), "lookLeft");
        am.put("lookLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnLookLeft.isVisible() && btnLookLeft.isEnabled())
                    btnLookLeft.doClick();
            }
        });

        im.put(KeyStroke.getKeyStroke("D"), "lookRight");
        am.put("lookRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnLookRight.isVisible() && btnLookRight.isEnabled())
                    btnLookRight.doClick();
            }
        });

        // Q — Mengintip lubang kunci (Peek Keyhole)
        im.put(KeyStroke.getKeyStroke("Q"), "peekKeyhole");
        am.put("peekKeyhole", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == GameState.GAMEOVER || (currentPosition != PlayerPosition.FRONT_ROOM && currentPosition != PlayerPosition.PEEKING_KEYHOLE)) return;
                
                if (currentPosition == PlayerPosition.PEEKING_KEYHOLE) {
                    currentPosition = PlayerPosition.FRONT_ROOM;
                    logEvent("👀 Kamu berhenti mengintip lubang kunci.");
                    // Reset Enemy A if it was in phase 1 or 2
                    if (enemyA.isAtDoor() && (enemyA.getPatienceTimer() == 2 || enemyA.getPatienceTimer() == 1)) {
                        logEvent("👤 [SAFE] Kamu berhenti mengintip. The Red One pergi.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        startRetreatAnimation(enemyA);
                    }
                } else {
                    currentPosition = PlayerPosition.PEEKING_KEYHOLE;
                    logEvent("👀 Kamu mengintip lubang kunci...");
                }
                officePanel.repaint();
            }
        });

        // E — Mengintip ventilasi (Peek Vent)
        im.put(KeyStroke.getKeyStroke("E"), "peekVent");
        am.put("peekVent", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == GameState.GAMEOVER || (currentPosition != PlayerPosition.FRONT_ROOM && currentPosition != PlayerPosition.PEEKING_VENT)) return;
                
                if (currentPosition != PlayerPosition.PEEKING_VENT) {
                    currentPosition = PlayerPosition.PEEKING_VENT;
                    logEvent("👀 Kamu mengecek ventilasi...");
                    // Retreat Enemy B if in phase 2
                    if (enemyB.isAtDoor() && enemyB.getPatienceTimer() == 2) {
                        logEvent("👤 [SAFE] Kamu memergoki Hina di ventilasi! Dia mundur.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        startRetreatAnimation(enemyB);
                    }
                } else {
                    currentPosition = PlayerPosition.FRONT_ROOM;
                    logEvent("👀 Kamu berhenti mengecek ventilasi.");
                }
                officePanel.repaint();
            }
        });

        // F — Senter (Flashlight)
        im.put(KeyStroke.getKeyStroke("F"), "toggleFlashlight");
        am.put("toggleFlashlight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == GameState.GAMEOVER || currentPosition != PlayerPosition.FRONT_ROOM) return;
                isFlashlightOn = !isFlashlightOn;
                logEvent("🔦 Senter: " + (isFlashlightOn ? "NYALA" : "MATI"));
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
                officePanel.repaint();
            }
        });

        // W — Mengumpat (masuk kabinet saat di ruang belakang)
        im.put(KeyStroke.getKeyStroke("W"), "hideCabinet");
        am.put("hideCabinet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((currentState == GameState.GAMEOVER) || !(currentPosition == PlayerPosition.BACK_ROOM) || (currentPosition == PlayerPosition.CABINET) || lockpickPopupPanel.isVisible())
                    return;
                enterCabinet();
            }
        });

        // S — Multi-aksi kontekstual:
        // • Di ruang belakang (tidak bersembunyi, tidak lockpick) → buka lockpick pintu
        // • Sedang di layar lockpick → tutup lockpick / mundur
        // • Sedang bersembunyi (tidak struggle) → keluar kabinet
        // ESC → selalu mundur / tutup
        AbstractAction sAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((currentState == GameState.GAMEOVER))
                    return;

                if (lockpickPopupPanel.isVisible()) {
                    // Tutup lockpick + stop QTE
                    stopQte();
                    lockpickPopupPanel.setVisible(false);
                    updateUIVisibility();
                    logEvent("🔧 [INTERACT] Kamu mundur dari gembok.");
                } else if ((currentPosition == PlayerPosition.CABINET) && !(currentState == GameState.STRUGGLING)) {
                    // Keluar kabinet
                    exitCabinet();
                } else if ((currentPosition == PlayerPosition.BACK_ROOM) && !(currentPosition == PlayerPosition.CABINET)) {
                    // Buka lockpick pintu belakang + start QTE
                    logEvent("🔧 [INTERACT] Mendekat ke gembok untuk mencongkel...");
                    lockpickPopupPanel.setVisible(true);
                    startQte();
                    updateUIVisibility();
                }
            }
        };
        im.put(KeyStroke.getKeyStroke("S"), "sAction");
        am.put("sAction", sAction);

        // ESC — Universal "Back" / "Mundur" action
        AbstractAction escAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentState == GameState.GAMEOVER || currentState == GameState.STRUGGLING)
                    return;

                if (currentPosition == PlayerPosition.PEEKING_KEYHOLE) {
                    currentPosition = PlayerPosition.FRONT_ROOM;
                    logEvent("👀 Kamu berhenti mengintip lubang kunci.");
                    if (enemyA.isAtDoor() && (enemyA.getPatienceTimer() == 2 || enemyA.getPatienceTimer() == 1)) {
                        logEvent("👤 [SAFE] Kamu berhenti mengintip. The Red One pergi.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        startRetreatAnimation(enemyA);
                    }
                    officePanel.repaint();
                } else if (currentPosition == PlayerPosition.PEEKING_VENT) {
                    currentPosition = PlayerPosition.FRONT_ROOM;
                    logEvent("👀 Kamu berhenti mengecek ventilasi.");
                    officePanel.repaint();
                } else if (lockpickPopupPanel.isVisible()) {
                    stopQte();
                    lockpickPopupPanel.setVisible(false);
                    updateUIVisibility();
                    logEvent("🔧 [INTERACT] Kamu mundur dari gembok.");
                } else if (currentPosition == PlayerPosition.CABINET) {
                    exitCabinet();
                } else if (currentPosition == PlayerPosition.BACK_ROOM) {
                    currentPosition = PlayerPosition.FRONT_ROOM;
                    updateUIVisibility();
                    officePanel.repaint();
                }
            }
        };
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "escAction");
        am.put("escAction", escAction);
    }

    // ============================================================
    // SETUP GAME LOOP
    // ============================================================

    private void setupGameLoop() {
        gameLoopTimer = new Timer(1200, e -> processGameTick());

        lockDrainTimer = new Timer(3000, e -> {
            if (!(currentState == GameState.GAMEOVER) && !lockpickPopupPanel.isVisible() && lockBars > 0) {
                lockBars--;
                logEvent("⚠️ [PENALTY] Progres gembok menurun (" + lockBars + "/6 bar).");
            }
        });
    }

    private void processGameTick() {
        if ((currentState == GameState.GAMEOVER) || (currentState == GameState.STRUGGLING) || isRetreating)
            return;

        if (currentState == GameState.HALLWAY) {
            if (Math.random() < 0.1 && !isFlickering) {
                startFlickerEffect();
            }
            return;
        }

        if (areEnemiesActive) {
            // ✨ MODIFIKASI: Enemy A pause jika sedang mengintip
            if (enemyA.isAtDoor() || getEnemyAtDoor() == null) {
                if (currentPosition != PlayerPosition.PEEKING_KEYHOLE || !enemyA.isAtDoor()) {
                    enemyA.act();
                }
            }
            // ✨ MODIFIKASI: Enemy B selalu act() kecuali jika player sedang mengintip dan ia berada di phase 2 (langsung retreat)
            if (enemyB.isAtDoor() || getEnemyAtDoor() == null) {
                enemyB.act();
            }
            // (Jika sudah ada musuh di pintu, act() di atas tidak dipanggil kecuali untuk countdown di bawah)

            Enemy attacker = getEnemyAtDoor();
            if (attacker != null) {
                checkDoorDefense(attacker);
                // ✨ Update intensitas vignette berdasarkan sisa waktu (patience)
                int p = attacker.getPatienceTimer();
                if (p <= 1) vignetteIntensity = 0.8f;
                else if (p == 2) vignetteIntensity = 0.5f;
                else if (p == 3) vignetteIntensity = 0.2f;
                else vignetteIntensity = 0f;
            } else {
                vignetteIntensity = 0f;
            }

            updateDoorVisuals();
        }
    }

    // ============================================================
    // LOGIKA DEFENSE & SERANGAN ENEMY
    // ============================================================

    private void checkDoorDefense(Enemy enemy) {
        if (!enemy.isAtDoor())
            return;

        // ============================================================
        // ENEMY A (The Red One) — New Phase Flow
        // ============================================================
        if (enemy == enemyA) {
            if (enemy.getPatienceTimer() == 3) {
                // Pintu tunnel terbuka di awal tanpa sprite muncul (hanya audio)
                if (!hasFlickeredForEnemyA) {
                    startFlickerEffect();
                    hasFlickeredForEnemyA = true;
                }
                if (!isFlickering && player.isLeftDoorClosed()) {
                    logEvent("🚪 *KREK*... Pintu tunnel terbuka. (Phase Start)");
                    player.setLeftDoorClosed(false);
                    AudioManager.playSound("/assets/audio/sfx/door_open.wav");
                }
            }
            if (enemy.getPatienceTimer() == 2) {
                // Phase 1: Langkah kaki pertama
                logEvent("👣 *Tap tap tap*... Terdengar langkah kaki mendekat.");
                AudioManager.playSound("/assets/audio/sfx/footsteps.wav");
            }

            if (enemy.getPatienceTimer() == 1) {
                // Phase 2: Langkah kaki dekat lubang kunci
                if (!hasFlickeredForDanger) {
                    logEvent("👣 *TAP TAP*... Langkah kaki berhenti di dekat lubang kunci!");
                    AudioManager.playSound("/assets/audio/sfx/footsteps.wav");
                    startFlickerEffect();
                    hasFlickeredForDanger = true;
                }
            }

            if (enemy.getPatienceTimer() <= 0) {
                if (currentPosition == PlayerPosition.CABINET) {
                    if (hidEarly) {
                        logEvent("👤 [SAFE] Kamu bersembunyi sebelum " + enemy.getName() + " mendekat. Ia menyerah.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        startRetreatAnimation(enemy);
                        hidEarly = false;
                    } else {
                        startStruggle(enemy);
                    }
                } else {
                    initiateJumpscareSequence(enemy);
                }
            }
            return;
        }

        // ============================================================
        // ENEMY B (Hina) — 4-Phase Flow
        //   Phase 0 (patience=4): Vent Crawling — sound cue
        //   Phase 1 (patience=3): Show Up on Vent — partial visibility
        //   Phase 2 (patience=2): Idle in Front — fully visible
        //   Phase 3 (patience<=0): Jumpscare trigger
        // ============================================================

        // Phase 0: Vent Crawling — tension buildup
        if (enemy.getPatienceTimer() == 4) {
            logEvent("💨 *suara merangkak*... Ada sesuatu bergerak di dalam ventilasi.");
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav"); // Maybe vent crawl sound if available
        }

        // Phase 1: Show Up on Vent — right ventilation (idle_phase_1)
        if (enemy.getPatienceTimer() == 3) {
            logEvent("🕷️ Hina muncul di ventilasi sisi kanan...");
            updateDoorVisuals();
        }

        // Phase 2: Idle in Front — center vent (idle_phase_2)
        if (enemy.getPatienceTimer() == 2) {
            if (!hasFlickeredForPhase2) {
                startFlickerEffect();
                hasFlickeredForPhase2 = true;
            }
            logEvent("👁️ Hina berpindah ke tengah ventilasi! Dia menatapmu!");
            AudioManager.playSound("/assets/audio/sfx/door_close.wav");
            
            // Check if player is ALREADY peeking vent
            if (currentPosition == PlayerPosition.PEEKING_VENT) {
                logEvent("👤 [SAFE] Kamu memergoki Hina di ventilasi! Dia mundur.");
                AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                startRetreatAnimation(enemy);
            }
            updateDoorVisuals();
        }

        // Phase 2.5: Critical Danger (patience == 1)
        if (enemy.getPatienceTimer() == 1) {
            if (!hasFlickeredForDanger) {
                startFlickerEffect();
                hasFlickeredForDanger = true;
            }
        }

        // Phase 3: Execution (patience <= 0)
        if (enemy.getPatienceTimer() <= 0) {
            if (currentPosition == PlayerPosition.CABINET) {
                if (hidEarly) {
                    logEvent("👤 [SAFE] Kamu bersembunyi sebelum " + enemy.getName() + " mendekat. Ia menyerah.");
                    AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                    startRetreatAnimation(enemy);
                    hidEarly = false;
                } else {
                    startStruggle(enemy);
                }
            } else {
                initiateJumpscareSequence(enemy);
            }
        }
    }

    // ============================================================
    // SISTEM FLICKER (WARNING PHASE 2)
    // ============================================================

    private void startFlickerEffect() {
        isFlickering = true;
        flickerAlpha = 0f;
        AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");

        if (flickerTimer != null && flickerTimer.isRunning())
            flickerTimer.stop();

        flickerTimer = new Timer(50, new java.awt.event.ActionListener() {
            private int ticks = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                ticks++;
                // Flicker acak: 50% chance untuk gelap/terang
                if (Math.random() > 0.5) {
                    flickerAlpha = (float) (Math.random() * 0.7f);
                } else {
                    flickerAlpha = 0f;
                }

                if (ticks >= 20) { // Durasi 1 detik (20 * 50ms)
                    ((Timer) e.getSource()).stop();
                    isFlickering = false;
                    flickerAlpha = 0f;
                }
                officePanel.repaint();
            }
        });
        flickerTimer.start();
    }

    // ============================================================
    // STRUGGLE QTE SYSTEM
    // ============================================================

    private void startStruggle(Enemy enemy) {
        if ((currentState == GameState.STRUGGLING))
            return;

        if (!loadQteAssets(enemy)) {
            logEvent("❌ Gagal load asset QTE untuk " + enemy.getName() + "! Jumpscare paksa.");
            initiateJumpscareSequence(enemy);
            return;
        }

        currentState = GameState.STRUGGLING;
        currentAttacker = enemy;
        struggleValue = 40;
        struggleAnimCounter = 0;

        logEvent("⚠️ " + enemy.getName() + " MENEMUKANMU DAN MENARIK PINTU KABINET! TAHAN!");
        AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");

        doorEnemyVisual = null;
        ventEnemyVisual = null;
        officePanel.repaint();

        struggleTimer = new Timer(100, e -> {
            if (!(currentState == GameState.STRUGGLING) || (currentState == GameState.GAMEOVER)) {
                ((Timer) e.getSource()).stop();
                return;
            }
            struggleValue -= 5;
            struggleAnimCounter++;

            if (struggleValue <= 0) {
                ((Timer) e.getSource()).stop();
                currentState = GameState.PLAYING;
                currentPosition = PlayerPosition.BACK_ROOM;

                // ✨ FIX: Set (currentState == GameState.GAMEOVER) and stop timers immediately to prevent double jumpscare
                currentState = GameState.GAMEOVER;
                gameLoopTimer.stop();
                if (lockDrainTimer != null) lockDrainTimer.stop();

                // Hide controls to prevent interaction during jumpscare
                btnDoor.setVisible(false);
                btnLookLeft.setVisible(false);
                btnLookRight.setVisible(false);
                lockpickPopupPanel.setVisible(false);

                triggerJumpscare(currentAttacker);
                return;
            }
            officePanel.repaint();
        });
        struggleTimer.start();
    }

    private void checkStruggleWin() {
        if ((currentState == GameState.STRUGGLING) && struggleValue >= 100) {
            if (struggleTimer != null)
                struggleTimer.stop();
            currentState = GameState.PLAYING;
            struggleValue = 100;
            logEvent("👤 [SAFE] Kamu berhasil menahan pintunya! " +
                    currentAttacker.getName() + " menyerah dan pergi.");
            AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");

            // ✨ Start Retreat Animation
            startRetreatAnimation(currentAttacker);

            hidEarly = false;
            updateDoorVisuals();
            officePanel.repaint();
        }
    }

    // ============================================================
    // RETREAT ANIMATION SYSTEM
    // ============================================================

    private void startRetreatAnimation(Enemy enemy) {
        if (isRetreating)
            return;

        this.lastDefeatedEnemy = enemy;
        this.isRetreating = true;
        this.retreatAnimTicks = 0;

        // Use the standard idle sprite for retreat (idle_failed removed)
        this.retreatImg = getIdleSprite(enemy);

        if (retreatTimer != null && retreatTimer.isRunning())
            retreatTimer.stop();

        retreatTimer = new Timer(50, e -> {
            retreatAnimTicks++;
            if (retreatAnimTicks >= HitboxConfig.RETREAT_DURATION_TICKS) {
                ((Timer) e.getSource()).stop();
                finishRetreat();
            }
            officePanel.repaint();
        });
        retreatTimer.start();
    }

    private void finishRetreat() {
        if (lastDefeatedEnemy != null) {
            lastDefeatedEnemy.retreat(10);
            if (lastDefeatedEnemy == enemyB) {
                hasFlickeredForPhase2 = false; // Reset agar bisa flicker lagi nanti
            }
            if (lastDefeatedEnemy == enemyA) {
                hasFlickeredForEnemyA = false;
            }
            hasFlickeredForDanger = false; // Reset danger flicker
        }
        isRetreating = false;
        lastDefeatedEnemy = null;
        retreatImg = null;
        updateDoorVisuals();
        officePanel.repaint();
    }

    // ============================================================
    // JUMPSCARE SYSTEM
    // ============================================================

    private void initiateJumpscareSequence(Enemy enemy) {
        if ((currentState == GameState.GAMEOVER))
            return;
        currentState = GameState.GAMEOVER;
        gameLoopTimer.stop();
        if (lockDrainTimer != null)
            lockDrainTimer.stop();
        lockpickPopupPanel.setVisible(false);

        btnDoor.setVisible(false);
        btnLookLeft.setVisible(false);
        btnLookRight.setVisible(false);

        // ✨ STEP 1: Paksa player kembali ke front room
        currentPosition = PlayerPosition.FRONT_ROOM;

        if (player.isLeftDoorClosed()) {
            logEvent("💥 *BAM!* Pintu depan didobrak paksa!");
            player.toggleLeftDoor();
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        } else {
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        }

        updateDoorVisuals();
        officePanel.repaint();

        // ✨ STEP 2: Jeda sebentar sebelum lampu berkedip (Cinematic pause)
        Timer pauseTimer = new Timer(600, e1 -> {
            // ✨ STEP 3: Flicker / Lampu berkedip
            startFlickerEffect();
            
            // ✨ STEP 4: Eksekusi Jumpscare setelah flicker selesai
            Timer jumpTimer = new Timer(1200, e2 -> triggerJumpscare(enemy));
            jumpTimer.setRepeats(false);
            jumpTimer.start();
        });
        pauseTimer.setRepeats(false);
        pauseTimer.start();
    }

    private void triggerJumpscare(Enemy enemy) {
        doorEnemyVisual = null;
        ventEnemyVisual = null;
        officePanel.repaint();

        AudioManager.stopAllSounds();
        AudioManager.playSound("/assets/audio/sfx/jumpscare_scream.wav");

        // Pilih path GIF jumpscare berdasarkan enemy
        String imagePath;
        if (enemy == enemyA) {
            imagePath = "/assets/enemies/enemy_a_door/jumpscare/the-red-jumpscare.gif";
        } else {
            imagePath = "/assets/enemies/enemy_b_vent/jumpscare/hina_jumpscare.png";
        }

        JPanel jumpscarePanel = new JPanel() {
            private final Image jsImage;
            {
                setOpaque(false);
                URL imgUrl = getClass().getResource(imagePath);
                jsImage = (imgUrl != null) ? new javax.swing.ImageIcon(imgUrl).getImage() : null;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (jsImage != null) {
                    Rectangle b = RenderEngine.getGameBounds(getWidth(), getHeight());
                    g.drawImage(jsImage, b.x, b.y, b.width, b.height, this);
                }
            }
        };

        jumpscarePanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        layeredPane.add(jumpscarePanel, JLayeredPane.DRAG_LAYER); // ✨ Ganti ke DRAG_LAYER agar paling atas
        layeredPane.revalidate();
        layeredPane.repaint();

        Timer delayTimer = new Timer(1200, e -> {
            layeredPane.remove(jumpscarePanel);
            layeredPane.revalidate();
            layeredPane.repaint();
            endGame("GAME OVER", "Kamu diterkam oleh " + enemy.getName(), Color.RED, enemy);
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    // ============================================================
    // END GAME
    // ============================================================

    private void endGame(String title, String msg, Color titleColor, Enemy killer) {
        if (!(currentState == GameState.GAMEOVER))
            currentState = GameState.GAMEOVER;
        gameLoopTimer.stop();
        if (lockDrainTimer != null)
            lockDrainTimer.stop();
        lockpickPopupPanel.setVisible(false);

        endTitleLabel.setText(title);
        endTitleLabel.setForeground(titleColor);
        endMessageLabel.setText(msg);

        endScreenPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        endScreenPanel.setVisible(true);
        endScreenPanel.revalidate();
        endScreenPanel.repaint();

        if (killer != null && killer.getQuotePath() != null) {
            quoteTimer = new Timer(1500, e -> AudioManager.playSound(killer.getQuotePath()));
            quoteTimer.setRepeats(false);
            quoteTimer.start();
        }
    }

    // ============================================================
    // RESET GAME
    // ============================================================

    private void resetGame() {
        stopAllTimers();
        AudioManager.stopAllSounds();

        initGameData();
        endScreenPanel.setVisible(false);
        lockpickPopupPanel.setVisible(false);

        btnDoor.setVisible(true);
        btnLookLeft.setVisible(true);
        btnLookRight.setVisible(true);
        btnDoor.setText("🚪 TUTUP PINTU DEPAN [E]");

        statusLabel.setText(
                "Objective: Bobol rantai pintu belakang sebelum mereka menangkapmu. (F11 = Maximize)");
        statusLabel.setForeground(Color.YELLOW);

        updateDoorVisuals();
        updateUIVisibility();
        gameLoopTimer.start();
        if (lockDrainTimer != null)
            lockDrainTimer.start();
    }

    /** Hentikan semua timer secara aman (cegah timer leak). */
    private void stopAllTimers() {
        if (quoteTimer != null && quoteTimer.isRunning())
            quoteTimer.stop();
        if (struggleTimer != null && struggleTimer.isRunning())
            struggleTimer.stop();
        if (lockDrainTimer != null && lockDrainTimer.isRunning())
            lockDrainTimer.stop();
        if (gameLoopTimer != null && gameLoopTimer.isRunning())
            gameLoopTimer.stop();
        if (flickerTimer != null && flickerTimer.isRunning())
            flickerTimer.stop();
        if (typingTimer != null && typingTimer.isRunning())
            typingTimer.stop();
    }

    // ============================================================
    // UTILITY
    // ============================================================

    public void startGame() {
        this.requestFocusInWindow();
        updateUIVisibility();

        if (currentState == GameState.INCOMING) {
            // Babak Incoming: enemies tidak aktif, ada dialog singkat
            incomingDialogVisible = true;
            logEvent("🎬 [INCOMING] Prolog dimulai...");
            
            // Timer 7 detik untuk dialog prolog, setelah itu game asli dimulai
            incomingTimer = new Timer(7000, e -> switchToActualGame());
            incomingTimer.setRepeats(false);
            incomingTimer.start();
        } else {
            gameLoopTimer.start();
            if (lockDrainTimer != null)
                lockDrainTimer.start();
        }
    }

    /**
     * Transisi dari babak Incoming ke Actual Game (PLAYING).
     */
    private void switchToActualGame() {
        if (currentState != GameState.INCOMING) return;
        
        if (incomingTimer != null && incomingTimer.isRunning())
            incomingTimer.stop();
            
        currentState = GameState.PLAYING;
        areEnemiesActive = true;
        incomingDialogVisible = false;
        
        logEvent("🎮 [ACTUAL GAME] Shift malam dimulai! Musuh aktif.");
        
        if (!gameLoopTimer.isRunning()) gameLoopTimer.start();
        if (!lockDrainTimer.isRunning()) lockDrainTimer.start();
        
        officePanel.repaint();
    }

    private void handleHallwayClick(Point gamePoint) {
        if (RenderEngine.hitboxContains(HitboxConfig.HALLWAY_CABINET_HITBOX, gamePoint) ||
            RenderEngine.hitboxContains(HitboxConfig.HALLWAY_TABLE_HITBOX, gamePoint)) {
            if (!hasHallwayKey) {
                hasHallwayKey = true;
                logEvent("🗝️ [ITEM] Kamu menemukan kunci!");
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            } else {
                logEvent("🔍 Kamu sudah mengambil kunci dari sini.");
            }
        } else if (RenderEngine.hitboxContains(HitboxConfig.HALLWAY_DOOR_HITBOX, gamePoint)) {
            if (hasHallwayKey) {
                logEvent("🚪 [INTERACT] Membuka pintu besar...");
                AudioManager.playSound("/assets/audio/sfx/door_open.wav");
                startHallwayCutscene();
            } else {
                logEvent("🔒 Pintu terkunci. Kamu butuh kunci.");
            }
        }
        officePanel.repaint();
    }

    private void startHallwayCutscene() {
        hallwayCutsceneActive = true;
        hallwayCutsceneIndex = 0;
        incomingDialogVisible = false;
        startTypingText();
        officePanel.repaint();
    }

    private void advanceHallwayCutscene() {
        if (typingTimer != null && typingTimer.isRunning()) {
            typingTimer.stop();
            currentDisplayedText = hallwayCutsceneTexts[hallwayCutsceneIndex];
        } else {
            hallwayCutsceneIndex++;
            if (hallwayCutsceneIndex >= hallwayCutsceneTexts.length) {
                hallwayCutsceneActive = false;
                currentState = GameState.INCOMING; // Transition to original prolog
                startGame(); // Restart timing for incoming
            } else {
                startTypingText();
            }
        }
        officePanel.repaint();
    }

    private void startTypingText() {
        if (typingTimer != null && typingTimer.isRunning()) typingTimer.stop();
        currentDisplayedText = "";
        typingCharIndex = 0;
        String targetText = hallwayCutsceneTexts[hallwayCutsceneIndex];
        
        typingTimer = new Timer(50, e -> {
            if (typingCharIndex < targetText.length()) {
                currentDisplayedText += targetText.charAt(typingCharIndex);
                typingCharIndex++;
                officePanel.repaint();
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        typingTimer.start();
    }

    private void updateUIVisibility() {
        if ((currentState == GameState.GAMEOVER))
            return;
        boolean isLockpickOpen = lockpickPopupPanel.isVisible();
        boolean isFront = !(currentPosition == PlayerPosition.BACK_ROOM);
        boolean isHallway = (currentState == GameState.HALLWAY);

        btnDoor.setVisible(false); // ✨ Selalu false sesuai permintaan (player tidak kontrol pintu)
        btnLookLeft.setVisible(!isLockpickOpen && !(currentPosition == PlayerPosition.CABINET) && !isHallway);
        btnLookRight.setVisible(!isLockpickOpen && !(currentPosition == PlayerPosition.CABINET) && !isHallway);
    }

    private Enemy getEnemyAtDoor() {
        if (enemyA.isAtDoor())
            return enemyA;
        if (enemyB.isAtDoor())
            return enemyB;
        return null;
    }

    private void logEvent(String message) {
        System.out.println(message);
        if (MainFrame.isDevMode) {
            devLogs.add(0, message);
            if (devLogs.size() > 10)
                devLogs.remove(devLogs.size() - 1);
            if (officePanel != null)
                officePanel.repaint();
        }
    }

    @Override
    public void stopAllProcesses() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) gameLoopTimer.stop();
        if (lockDrainTimer != null && lockDrainTimer.isRunning()) lockDrainTimer.stop();
        if (retreatTimer != null && retreatTimer.isRunning()) retreatTimer.stop();
        if (quoteTimer != null && quoteTimer.isRunning()) quoteTimer.stop();
        if (flickerTimer != null && flickerTimer.isRunning()) flickerTimer.stop();
    }

    // ============================================================
    // GETTERS FOR RENDERER
    // ============================================================
    public GameState getCurrentState() { return currentState; }
    public PlayerPosition getCurrentPosition() { return currentPosition; }
    public float getVignetteIntensity() { return vignetteIntensity; }
    public boolean isFlickering() { return isFlickering; }
    public float getFlickerAlpha() { return flickerAlpha; }
    public boolean isRetreating() { return isRetreating; }
    public EnemyOdd getEnemyA() { return enemyA; }
    public EnemyEven getEnemyB() { return enemyB; }
    public Image getDoorEnemyVisual() { return doorEnemyVisual; }
    public Player getPlayer() { return player; }
    public int getLockBars() { return lockBars; }
    public Image getRetreatImg() { return retreatImg; }
    public Enemy getLastDefeatedEnemy() { return lastDefeatedEnemy; }
    public int getRetreatAnimTicks() { return retreatAnimTicks; }
    public Image getQteBodyImg() { return qteBodyImg; }
    public Image getQteHandLeftImg() { return qteHandLeftImg; }
    public Image getQteHandRightImg() { return qteHandRightImg; }
    public int getStruggleValue() { return struggleValue; }
    public java.util.List<String> getDevLogs() { return devLogs; }
    public boolean isIncomingDialogVisible() { return incomingDialogVisible; }
    
    // ✨ Getters for Hallway Phase
    public boolean isHallwayCutsceneActive() { return hallwayCutsceneActive; }
    public int getHallwayCutsceneIndex() { return hallwayCutsceneIndex; }
    public String[] getHallwayCutsceneTexts() { return hallwayCutsceneTexts; }
    public boolean hasHallwayKey() { return hasHallwayKey; }
    public String getCurrentDisplayedText() { return currentDisplayedText; }
    
    public boolean isPeekingKeyhole() { return currentPosition == PlayerPosition.PEEKING_KEYHOLE; }
    public boolean isPeekingVent() { return currentPosition == PlayerPosition.PEEKING_VENT; }
    public boolean isFlashlightOn() { return isFlashlightOn; }
}
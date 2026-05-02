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
    private boolean isGameOver = false;

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
    private boolean isHidden = false;
    private boolean isStruggling = false;
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
    private boolean isLookingBack = false;
    private float vignetteIntensity = 0f; // ✨ BARU: Intensitas efek vignette
    private boolean isFlickering = false; // ✨ BARU: Status lampu mati-nyala
    private float flickerAlpha = 0f;      // ✨ BARU: Opacity overlay hitam saat flicker
    private Timer flickerTimer;           // ✨ BARU: Timer untuk kontrol flicker
    private boolean hasFlickeredForPhase2 = false; // ✨ BARU: Flag agar flicker hanya 1x per approach
    private boolean hasFlickeredForEnemyA = false; // ✨ BARU: Flag agar flicker hanya 1x per approach (The Red One)
    private boolean hasFlickeredForDanger = false; // ✨ BARU: Flag untuk flicker peringatan terakhir (patience 1)
    private java.util.List<String> devLogs = new java.util.ArrayList<>(); // ✨ BARU: Cache log untuk Dev Mode

    // Path konstanta aset ruangan
    private static final String PATH_FRONT_ROOM = "/assets/rooms/front_room.png";
    private static final String PATH_FRONT_DOOR = "/assets/rooms/front_door.png";
    private static final String PATH_BACK_DOOR = "/assets/rooms/back_door.png";
    private static final String PATH_BACK_DOOR_OPENED = "/assets/rooms/back_door_opened.png";
    private static final String PATH_LOCK_DOOR = "/assets/rooms/lock_door.png";

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

        this.areEnemiesActive = true;
        this.isGameOver = false;
        this.isLookingBack = false;
        this.lockBars = 0;
        this.qteIndicatorPos = 0.0;
        this.qteDirection = 1;
        this.qteActive = false;
        this.isHidden = false;
        this.isStruggling = false;
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
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png",
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-2.png",
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
        officePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintGame((Graphics2D) g);
            }
        };
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
    // RENDERING UTAMA
    // ============================================================

    /**
     * Dipanggil dari paintComponent officePanel.
     * Semua logika render ada di sini agar terstruktur.
     */
    private void paintGame(Graphics2D g2d) {
        int pw = officePanel.getWidth();
        int ph = officePanel.getHeight();

        // Background hitam (letterbox)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, pw, ph);

        // Ambil batas cangkang
        Rectangle bounds = RenderEngine.getGameBounds(pw, ph);

        if (isLookingBack) {
            paintBackRoom(g2d, bounds);
        } else {
            paintFrontRoom(g2d, bounds);
        }

        if (isHidden) {
            paintCabinetView(g2d, bounds, pw, ph);
        }

        // ✨ BARU: Render vignette indicator
        if (vignetteIntensity > 0) {
            paintVignette(g2d, pw, ph);
        }

        // ✨ BARU: Render overlay flicker lampu
        if (isFlickering) {
            g2d.setColor(new Color(0, 0, 0, (int) (flickerAlpha * 255)));
            g2d.fillRect(0, 0, pw, ph);
        }

        // ✨ BARU: Render overlay animasi retreat (saat musuh kabur)
        if (isRetreating) {
            paintRetreatOverlay(g2d, bounds);
        }

        // ✨ BARU: Render log developer di pojok kiri atas
        if (MainFrame.isDevMode) {
            paintDevLogs(g2d);
        }
    }

    private void paintFrontRoom(Graphics2D g2d, Rectangle bounds) {
        // 1. Render Enemy B Phase 1: Show Up on Vent (patience == 3)
        // Hina partially visible in the vent opening
        if (enemyB.isAtDoor() && enemyB.getPatienceTimer() == 3 && !isStruggling && !isRetreating) {
            Image ventSprite = AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
            if (ventSprite != null) {
                RenderEngine.drawSprite(g2d, ventSprite, bounds,
                        HitboxConfig.ENEMY_B_SPRITE_X, HitboxConfig.ENEMY_B_SPRITE_Y,
                        HitboxConfig.ENEMY_B_SPRITE_W, HitboxConfig.ENEMY_B_SPRITE_H,
                        true, officePanel);
            }
        }

        // 2. Render Enemy A (The Red One - Pintu Depan)
        // ✨ MODIFIKASI: Hanya muncul JIKA tidak sedang flicker
        if (doorEnemyVisual != null && !player.isLeftDoorClosed() && !isFlickering) {
            RenderEngine.drawSprite(g2d, doorEnemyVisual, bounds,
                    HitboxConfig.ENEMY_A_SPRITE_X, HitboxConfig.ENEMY_A_SPRITE_Y,
                    HitboxConfig.ENEMY_A_SPRITE_W, HitboxConfig.ENEMY_A_SPRITE_H,
                    true, officePanel);
        }

        // 3. Render Enemy B Phase 2: Idle in Front (patience <= 2)
        // Hina fully visible, standing in front of the player
        // ✨ MODIFIKASI: Hanya muncul solid JIKA flicker sudah selesai
        if (enemyB.isAtDoor() && enemyB.getPatienceTimer() <= 2 && !isStruggling && !isRetreating && !isFlickering) {
            Image hinaImg = getIdleSprite(enemyB);
            if (hinaImg != null) {
                RenderEngine.drawSprite(g2d, hinaImg, bounds,
                        HitboxConfig.ENEMY_B_PHASE2_X, HitboxConfig.ENEMY_B_PHASE2_Y,
                        HitboxConfig.ENEMY_B_PHASE2_W, HitboxConfig.ENEMY_B_PHASE2_H,
                        true, officePanel);
            }
        }

        // 4. Render ruangan depan (di atas enemy agar enemy tampak berada di balik
        // dinding)
        Image imgFront = AssetCache.get(PATH_FRONT_ROOM);
        if (imgFront != null) {
            g2d.drawImage(imgFront, bounds.x, bounds.y, bounds.width, bounds.height, officePanel);
        }

        // 5. Render pintu tertutup (overlay di atas ruangan)
        if (player.isLeftDoorClosed()) {
            Image imgDoor = AssetCache.get(PATH_FRONT_DOOR);
            if (imgDoor != null) {
                g2d.drawImage(imgDoor, bounds.x, bounds.y, bounds.width, bounds.height, officePanel);
            }
        }
    }

    private void paintBackRoom(Graphics2D g2d, Rectangle bounds) {
        // Tampilkan pintu belakang (terbuka atau terkunci)
        boolean isUnlocked = (lockBars >= 6);
        String path = isUnlocked ? PATH_BACK_DOOR_OPENED : PATH_BACK_DOOR;
        Image img = AssetCache.get(path);
        if (img != null) {
            g2d.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, officePanel);
        }

        // ============================================================
        // DEBUG HITBOX: Aktifkan baris-baris di bawah ini saat kalibrasi
        // ============================================================
        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.CABINET_HITBOX, bounds,
                new Color(0, 255, 0, 180));
        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.LOCKDOOR_HITBOX, bounds,
                new Color(255, 0, 0, 180));
    }

    private void paintCabinetView(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        if (isStruggling && qteBodyImg != null) {
            // ✨ OVERHAUL QTE: Delegasikan seluruh perenderan ke paintStruggleQTE untuk
            // kontrol layering
            paintStruggleQTE(g2d, bounds, pw, ph);
        } else {
            // === RENDER IDLE CABINET (Tanpa Struggle) ===
            int slitWidth = (int) (bounds.width * HitboxConfig.CABINET_SLIT_WIDTH_FRACTION);
            int slitX = bounds.x + (bounds.width - slitWidth) / 2;
            int slitMarginV = (int) (bounds.height * HitboxConfig.CABINET_SLIT_MARGIN_FRACTION);
            int slitY = bounds.y + slitMarginV;
            int slitH = bounds.height - slitMarginV * 2;

            // Blackout seluruh layar kecuali celah (Idle)
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, slitX, ph); // kiri
            g2d.fillRect(slitX + slitWidth, 0, pw - (slitX + slitWidth), ph); // kanan
            g2d.fillRect(slitX, 0, slitWidth, slitY); // atas celah
            g2d.fillRect(slitX, slitY + slitH, slitWidth, ph - (slitY + slitH)); // bawah celah

            // Teks panduan
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString(">>> MENGINTIP DARI DALAM KABINET <<<", 30, 40);
        }
    }

    /**
     * Render overlay musuh yang sedang kabur (retreat).
     */
    private void paintRetreatOverlay(Graphics2D g2d, Rectangle bounds) {
        if (retreatImg == null || lastDefeatedEnemy == null)
            return;

        // ✨ OVERHAUL: Efek Zoom-Out (Mengecil) saat mundur
        double progress = (double) retreatAnimTicks / HitboxConfig.RETREAT_DURATION_TICKS;
        double scaleFactor = 1.3 - (progress * 0.5); // Mulai dari 1.3x mengecil ke 0.8x
        
        // Hitung posisi horizontal berdasarkan tick (meluncur ke samping)
        int xOffset = retreatAnimTicks * HitboxConfig.RETREAT_SPEED_X;

        // Pilih posisi asal berdasarkan enemy (Hina di kanan, Red One di kiri)
        int basePosX, basePosY, baseW, baseH;
        if (lastDefeatedEnemy == enemyB) {
            // ✨ OVERHAUL HINA RETREAT: Berukuran besar dan bergerak ke arah KANAN
            double hinaScale = scaleFactor * 1.5; // Lebih besar sesuai permintaan
            basePosX = HitboxConfig.ENEMY_B_PHASE2_X + xOffset;
            basePosY = HitboxConfig.ENEMY_B_PHASE2_Y;
            baseW = (int) (HitboxConfig.ENEMY_B_PHASE2_W * hinaScale);
            baseH = (int) (HitboxConfig.ENEMY_B_PHASE2_H * hinaScale);
            
            // Render manual agar bisa "di belakang/luar" black bar jika perlu (clipping otomatis oleh Graphics context)
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, officePanel);
        } else {
            // Enemy A mundur ke arah kiri
            basePosX = HitboxConfig.ENEMY_A_SPRITE_X - xOffset;
            basePosY = HitboxConfig.ENEMY_A_SPRITE_Y;
            baseW = (int) (HitboxConfig.ENEMY_A_SPRITE_W * scaleFactor);
            baseH = (int) (HitboxConfig.ENEMY_A_SPRITE_H * scaleFactor);
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, officePanel);
        }
    }

    private void paintStruggleQTE(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        // 1. HITUNG DIMENSI CELAH (Berdasarkan Progress)
        // 100% (Menang) = Mingkem (Min Slit), 0% (Kalah) = Mangap (Max Slit)
        double progressRatio = struggleValue / 100.0;
        int slitWidth = (int) (HitboxConfig.QTE_SLIT_WIDTH_MAX -
                (progressRatio * (HitboxConfig.QTE_SLIT_WIDTH_MAX - HitboxConfig.QTE_SLIT_WIDTH_MIN)));

        int slitX = bounds.x + (bounds.width - slitWidth) / 2;
        int slitMarginV = (int) (bounds.height * HitboxConfig.CABINET_SLIT_MARGIN_FRACTION);
        int slitY = bounds.y + slitMarginV;
        int slitH = bounds.height - slitMarginV * 2;

        // 2. LAYER 1: Gambar tubuh musuh (PALING BELAKANG)
        // ✨ MODIFIKASI: Body dibuat lebih besar (1.1 -> 1.4) agar lebih realistis
        int bodyH = (int) (slitH * 1.4); 
        int bodyOrigW = qteBodyImg.getWidth(officePanel);
        int bodyOrigH = qteBodyImg.getHeight(officePanel);
        int bodyW = (bodyOrigH > 0) ? (bodyH * bodyOrigW / bodyOrigH) : slitWidth;
        int bodyX = bounds.x + (bounds.width - bodyW) / 2;
        int bodyY = slitY - (bodyH - slitH) / 2;

        g2d.drawImage(qteBodyImg, bodyX, bodyY, bodyW, bodyH, officePanel);

        // 3. LAYER 2: Gambar "Mask" Hitam (Pintu Kabinet)
        // Ini akan menutupi tubuh musuh di sisi kiri dan kanan
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, slitX, ph); // Bar Kiri
        g2d.fillRect(slitX + slitWidth, 0, pw - (slitX + slitWidth), ph); // Bar Kanan
        g2d.fillRect(slitX, 0, slitWidth, slitY); // Bar Atas
        g2d.fillRect(slitX, slitY + slitH, slitWidth, ph - (slitY + slitH)); // Bar Bawah

        // 4. LAYER 3: Gambar tangan musuh (PALING DEPAN — di atas black bars / screen edges)
        if (qteHandLeftImg != null && qteHandRightImg != null) {
            int handH = (int) (slitH * 0.85);
            int hOrigW = qteHandLeftImg.getWidth(officePanel);
            int hOrigH = qteHandLeftImg.getHeight(officePanel);
            int handW = (hOrigH > 0) ? (handH * hOrigW / hOrigH) : (slitWidth / 2);

            int handY = slitY + (slitH - handH) / 2;

            // ✨ REPOSITION: Tangan kiri ditempel ke tepi kiri layar (black bar area)
            // Ujung kanan tangan menyentuh slit edge
            int lx = slitX - handW + (handW / 5);
            g2d.drawImage(qteHandLeftImg, lx, handY, handW, handH, officePanel);

            // ✨ REPOSITION: Tangan kanan ditempel ke tepi kanan layar (black bar area)
            int rx = slitX + slitWidth - (handW / 5);
            g2d.drawImage(qteHandRightImg, rx, handY, handW, handH, officePanel);
        }

        // 5. Teks & Bar UI
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Consolas", Font.BOLD, 30));
        String warn = "!!! TAHAN PINTU !!!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(warn, (pw - fm.stringWidth(warn)) / 2, ph / 2 - 120);

        int barW = 500, barH = 25;
        int barX = (pw - barW) / 2;
        int barY = ph - 100;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(barX, barY, barW, barH);

        if (struggleValue < 30)
            g2d.setColor(Color.RED);
        else if (struggleValue < 70)
            g2d.setColor(Color.YELLOW);
        else
            g2d.setColor(Color.GREEN);
        g2d.fillRect(barX, barY, (int) ((progressRatio) * barW), barH);

        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barW, barH);

        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        String instText = "SPAM KLIK ATAU SPASI! [" + struggleValue + "%]";
        FontMetrics fmT = g2d.getFontMetrics();
        g2d.drawString(instText, (pw - fmT.stringWidth(instText)) / 2, barY - 15);
    }

    /**
     * Menggambar efek vignette (hitam di pinggir) sebagai penanda bahaya.
     */
    private void paintVignette(Graphics2D g2d, int w, int h) {
        float alpha = Math.min(0.8f, vignetteIntensity);
        RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f),
                Math.max(w, h) / 1.5f,
                new float[] { 0.0f, 0.8f, 1.0f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, (int) (alpha * 150)), new Color(0, 0, 0, (int) (alpha * 255)) }
        );
        g2d.setPaint(rgp);
        g2d.fillRect(0, 0, w, h);
    }

    /**
     * Render log kejadian secara real-time untuk Developer Mode.
     */
    private void paintDevLogs(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        int y = 30;
        for (String log : devLogs) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(log, 22, y + 2);
            // Text
            g2d.setColor(Color.CYAN);
            g2d.drawString(log, 20, y);
            y += 20;
        }
    }

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
                if (isGameOver)
                    return;
                requestFocusInWindow();

                // Prioritas 1: Struggle QTE - klik mana pun dihitung
                if (isStruggling) {
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
                if (isHidden) {
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

                // Prioritas 3: Interaksi di back room
                if (isLookingBack && !lockpickPopupPanel.isVisible()) {
                    handleBackRoomClick(gamePoint);
                }
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
        isHidden = true;
        isLookingBack = false;

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
        isHidden = false;
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
        qteIndicatorPos = 0.0;
        qteDirection = 1;
        qteActive = true;

        if (qteAnimTimer != null && qteAnimTimer.isRunning())
            qteAnimTimer.stop();

        // ~60fps animasi indikator
        qteAnimTimer = new Timer(16, e -> {
            if (!qteActive || isGameOver) {
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
        if (!qteActive || lockBars >= 6 || isGameOver)
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
        isGameOver = true;
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
    private Image getIdleSprite(Enemy enemy) {
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
                && !isStruggling && !isRetreating) {
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
            if (isGameOver || lockpickPopupPanel.isVisible() || isHidden)
                return;
            isLookingBack = !isLookingBack;
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
                if (isGameOver) return;
                // Prioritas 1: QTE Lockpick
                if (qteActive && lockpickPopupPanel.isVisible()) {
                    handleQteHit();
                    return;
                }
                // Prioritas 2: Struggle QTE
                if (isStruggling) {
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

        // E — Toggle pintu depan
        im.put(KeyStroke.getKeyStroke("E"), "toggleDoor");
        am.put("toggleDoor", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ✨ Player tidak bisa kontrol manual via E lagi.
            }
        });

        // W — Mengumpat (masuk kabinet saat di ruang belakang)
        im.put(KeyStroke.getKeyStroke("W"), "hideCabinet");
        am.put("hideCabinet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isGameOver || !isLookingBack || isHidden || lockpickPopupPanel.isVisible())
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
                if (isGameOver)
                    return;

                if (lockpickPopupPanel.isVisible()) {
                    // Tutup lockpick + stop QTE
                    stopQte();
                    lockpickPopupPanel.setVisible(false);
                    updateUIVisibility();
                    logEvent("🔧 [INTERACT] Kamu mundur dari gembok.");
                } else if (isHidden && !isStruggling) {
                    // Keluar kabinet
                    exitCabinet();
                } else if (isLookingBack && !isHidden) {
                    // Buka lockpick pintu belakang + start QTE
                    logEvent("🔧 [INTERACT] Mendekat ke gembok untuk mencongkel...");
                    lockpickPopupPanel.setVisible(true);
                    startQte();
                    updateUIVisibility();
                }
            }
        };
        im.put(KeyStroke.getKeyStroke("S"), "sAction");
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "sAction");
        am.put("sAction", sAction);
    }

    // ============================================================
    // SETUP GAME LOOP
    // ============================================================

    private void setupGameLoop() {
        gameLoopTimer = new Timer(1200, e -> processGameTick());

        lockDrainTimer = new Timer(3000, e -> {
            if (!isGameOver && !lockpickPopupPanel.isVisible() && lockBars > 0) {
                lockBars--;
                logEvent("⚠️ [PENALTY] Progres gembok menurun (" + lockBars + "/6 bar).");
            }
        });
    }

    private void processGameTick() {
        if (isGameOver || isStruggling || isRetreating)
            return;

        if (areEnemiesActive) {
            // ✨ MODIFIKASI: Logika "Satu Per Satu" — Musuh hanya bisa maju jika pintu kosong.
            // Jika sudah di pintu, mereka tetap act() untuk countdown.
            if (enemyA.isAtDoor() || getEnemyAtDoor() == null) {
                enemyA.act();
            }
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
        // ENEMY A (The Red One) — 3-Phase Flow (unchanged)
        // ============================================================
        if (enemy == enemyA) {
            if (enemy.getPatienceTimer() == 3) {
                // ✨ BARU: Trigger flicker sebelum pintu terbuka
                if (!hasFlickeredForEnemyA) {
                    startFlickerEffect();
                    hasFlickeredForEnemyA = true;
                }

                // Pintu hanya terbuka setelah flicker (untuk efek dramatis)
                if (!isFlickering && player.isLeftDoorClosed()) {
                    logEvent("🚪 *KREK*... Pintu terbuka di PHASE 1. The Red One terlihat!");
                    player.setLeftDoorClosed(false);
                    AudioManager.playSound("/assets/audio/sfx/door_open.wav");
                }
            }
            if (enemy.getPatienceTimer() == 2) {
                logEvent("💥 Pintu tertutup kembali saat enemy mendekat...");
                player.setLeftDoorClosed(true);
                AudioManager.playSound("/assets/audio/sfx/door_close.wav");
                updateDoorVisuals();
                hidEarly = isHidden;
            }

            // Phase 3: Critical Danger (patience == 1)
            if (enemy.getPatienceTimer() == 1) {
                if (!hasFlickeredForDanger) {
                    startFlickerEffect();
                    hasFlickeredForDanger = true;
                }
            }

            if (enemy.getPatienceTimer() <= 0) {
                if (isHidden) {
                    if (hidEarly) {
                        logEvent("👤 [SAFE] Kamu sudah bersembunyi tepat waktu. The Red One pergi.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        enemy.retreat(10);
                        updateDoorVisuals();
                        hidEarly = false;
                    } else {
                        initiateJumpscareSequence(enemy);
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
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        }

        // Phase 1: Show Up on Vent — partially visible
        // If player is ALREADY hiding at this point, they're safe (no QTE)
        if (enemy.getPatienceTimer() == 3) {
            logEvent("🕷️ Hina muncul sebagian dari lubang ventilasi...");
            if (isHidden) {
                hidEarly = true; // Player hid before Idle in Front → safe
            }
            updateDoorVisuals();
        }

        // Phase 2: Idle in Front — fully visible, tension peak
        if (enemy.getPatienceTimer() == 2) {
            // ✨ BARU: Trigger efek flicker lampu sebelum muncul solid
            if (!hasFlickeredForPhase2) {
                startFlickerEffect();
                hasFlickeredForPhase2 = true;
            }

            logEvent("👁️ Hina berdiri di depanmu! Dia menatapmu dari celah ventilasi!");
            AudioManager.playSound("/assets/audio/sfx/door_close.wav");
            // Only mark as hidEarly if player was already hiding BEFORE this phase
            if (isHidden && !hidEarly) {
                hidEarly = false; // Player hid too late → QTE will trigger
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
            if (isHidden) {
                if (hidEarly) {
                    // Player hid during Phase 1 (Show Up on Vent) → safe, NO QTE
                    logEvent("👤 [SAFE] Kamu bersembunyi sebelum Hina mendekat. Dia kembali ke vent.");
                    AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                    enemy.retreat(10);
                    updateDoorVisuals();
                    hidEarly = false;
                } else {
                    // Player hid during Phase 2 (Idle in Front) → QTE triggers
                    startStruggle(enemy);
                }
            } else {
                // Player not hiding at all → direct jumpscare
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
        if (isStruggling)
            return;

        if (!loadQteAssets(enemy)) {
            logEvent("❌ Gagal load asset QTE untuk " + enemy.getName() + "! Jumpscare paksa.");
            initiateJumpscareSequence(enemy);
            return;
        }

        isStruggling = true;
        currentAttacker = enemy;
        struggleValue = 40;
        struggleAnimCounter = 0;

        logEvent("⚠️ " + enemy.getName() + " MENEMUKANMU DAN MENARIK PINTU KABINET! TAHAN!");
        AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");

        doorEnemyVisual = null;
        ventEnemyVisual = null;
        officePanel.repaint();

        struggleTimer = new Timer(100, e -> {
            if (!isStruggling || isGameOver) {
                ((Timer) e.getSource()).stop();
                return;
            }
            struggleValue -= 5;
            struggleAnimCounter++;

            if (struggleValue <= 0) {
                ((Timer) e.getSource()).stop();
                isStruggling = false;
                isHidden = false;

                // ✨ FIX: Set isGameOver and stop timers immediately to prevent double jumpscare
                isGameOver = true;
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
        if (isStruggling && struggleValue >= 100) {
            if (struggleTimer != null)
                struggleTimer.stop();
            isStruggling = false;
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
        if (isGameOver)
            return;
        isGameOver = true;
        gameLoopTimer.stop();
        if (lockDrainTimer != null)
            lockDrainTimer.stop();
        lockpickPopupPanel.setVisible(false);

        btnDoor.setVisible(false);
        btnLookLeft.setVisible(false);
        btnLookRight.setVisible(false);

        if (player.isLeftDoorClosed()) {
            logEvent("💥 *BAM!* Pintu depan didobrak paksa!");
            player.toggleLeftDoor();
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        } else {
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        }

        updateDoorVisuals();
        startFlickerEffect(); // ✨ Tambahkan flicker sebagai peringatan terakhir 1 detik
        officePanel.repaint();

        Timer peekTimer = new Timer(1000, e -> triggerJumpscare(enemy));
        peekTimer.setRepeats(false);
        peekTimer.start();
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
        if (!isGameOver)
            isGameOver = true;
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
    }

    // ============================================================
    // UTILITY
    // ============================================================

    public void startGame() {
        this.requestFocusInWindow();
        updateUIVisibility();
        gameLoopTimer.start();
        if (lockDrainTimer != null)
            lockDrainTimer.start();
    }

    private void updateUIVisibility() {
        if (isGameOver)
            return;
        boolean isLockpickOpen = lockpickPopupPanel.isVisible();
        boolean isFront = !isLookingBack;

        btnDoor.setVisible(false); // ✨ Selalu false sesuai permintaan (player tidak kontrol pintu)
        btnLookLeft.setVisible(!isLockpickOpen && !isHidden);
        btnLookRight.setVisible(!isLockpickOpen && !isHidden);
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
}
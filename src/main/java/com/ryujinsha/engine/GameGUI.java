package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AudioManager;
import com.ryujinsha.system.ResourceManaged;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * ✨ GameGUI — Layar Permainan Utama (Versi Modular)
 *
 * Bertindak sebagai orchestrator yang mendelegasikan logika ke subsystem:
 * - GameContext   → Shared state container
 * - InputHandler  → Key bindings & mouse input
 * - LockpickSystem → QTE gembok
 * - CombatSystem  → Struggle, retreat, jumpscare, flicker
 * - HallwaySystem → Hallway phase & cutscene
 * - GameRenderer  → Rendering visual (sudah ada sebelumnya)
 */
public class GameGUI extends JPanel implements ResourceManaged {

    // ============================================================
    // SUBSYSTEMS
    // ============================================================
    private final GameContext ctx;
    private final InputHandler inputHandler;
    private final LockpickSystem lockpickSystem;
    private final CombatSystem combatSystem;
    private final HallwaySystem hallwaySystem;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public GameGUI(MainFrame mainFrame) {
        ctx = new GameContext(mainFrame);

        // Inisialisasi subsystems
        lockpickSystem = new LockpickSystem(ctx);
        combatSystem = new CombatSystem(ctx);
        hallwaySystem = new HallwaySystem(ctx);
        inputHandler = new InputHandler(ctx);

        // Wire cross-references
        lockpickSystem.setHallwaySystem(hallwaySystem);
        inputHandler.setSystems(combatSystem, lockpickSystem, hallwaySystem);
        hallwaySystem.setInputHandler(inputHandler, this);

        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        ctx.layeredPane = new JLayeredPane();
        ctx.layeredPane.setBackground(Color.BLACK);
        ctx.layeredPane.setOpaque(true);
        add(ctx.layeredPane, BorderLayout.CENTER);

        setupUI();
        setupResponsiveListener();
        setupGameLoop();
        inputHandler.setupKeyBindings(this);
    }

    // ============================================================
    // SETUP UI
    // ============================================================

    private void setupUI() {
        // Status bar atas
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.DARK_GRAY);
        ctx.statusLabel = new JLabel(
                "Objective: Bobol gembok pintu belakang.  [A/D]=Lihat  [W]=Sembunyi  [S]=Gembok  [SPASI]=Hit  [ESC]=Keluar");
        ctx.statusLabel.setForeground(Color.YELLOW);
        ctx.statusLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        topPanel.add(ctx.statusLabel);
        add(topPanel, BorderLayout.NORTH);

        // Panel render utama
        ctx.officePanel = new GameRenderer(ctx);
        ctx.layeredPane.add(ctx.officePanel, JLayeredPane.DEFAULT_LAYER);

        // Mouse interactions
        inputHandler.setupInteractionHits(ctx.officePanel);

        // Lockpick popup
        JPanel lockpickPanel = lockpickSystem.setupLockpickUI();
        ctx.layeredPane.add(lockpickPanel, JLayeredPane.MODAL_LAYER);
        ctx.lockpickPopupPanel.setVisible(false);

        // End screen
        setupEndScreen();
        ctx.layeredPane.add(ctx.endScreenPanel, JLayeredPane.POPUP_LAYER);
        ctx.endScreenPanel.setVisible(false);

        // Floating controls
        setupFloatingControls();
    }

    // ============================================================
    // SETUP END SCREEN
    // ============================================================

    private void setupEndScreen() {
        ctx.endScreenPanel = new JPanel();
        ctx.endScreenPanel.setBackground(new Color(0, 0, 0, 220));
        ctx.endScreenPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 20, 0);

        ctx.endTitleLabel = new JLabel("GAME OVER", SwingConstants.CENTER);
        ctx.endTitleLabel.setFont(new Font("Consolas", Font.BOLD, 60));
        ctx.endScreenPanel.add(ctx.endTitleLabel, gbc);

        ctx.endMessageLabel = new JLabel("Message", SwingConstants.CENTER);
        ctx.endMessageLabel.setFont(new Font("Consolas", Font.PLAIN, 20));
        ctx.endMessageLabel.setForeground(Color.WHITE);
        ctx.endScreenPanel.add(ctx.endMessageLabel, gbc);

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
            ctx.mainFrame.showScreen("MENU");
        });

        btnPanel.add(btnRetry);
        btnPanel.add(btnMenu);
        ctx.endScreenPanel.add(btnPanel, gbc);
    }

    // ============================================================
    // SETUP FLOATING CONTROLS
    // ============================================================

    private void setupFloatingControls() {
        ctx.btnDoor = new PixelButton("🚪 TUTUP PINTU DEPAN [E]");
        ctx.btnLookLeft = new PixelButton("◀ [A]");
        ctx.btnLookRight = new PixelButton("▶ [D]");
        ctx.btnPeekKeyhole = new PixelButton("INTIP LUBANG KUNCI [Q]");
        ctx.btnPeekVent = new PixelButton("INTIP VENTILASI [E]");

        ctx.layeredPane.add(ctx.btnDoor, JLayeredPane.MODAL_LAYER);
        ctx.layeredPane.add(ctx.btnLookLeft, JLayeredPane.MODAL_LAYER);
        ctx.layeredPane.add(ctx.btnLookRight, JLayeredPane.MODAL_LAYER);
        ctx.layeredPane.add(ctx.btnPeekKeyhole, JLayeredPane.MODAL_LAYER);
        ctx.layeredPane.add(ctx.btnPeekVent, JLayeredPane.MODAL_LAYER);

        java.awt.event.ActionListener lookAction = e -> {
            if (ctx.currentState == GameState.GAMEOVER || ctx.lockpickPopupPanel.isVisible() ||
                ctx.currentPosition == PlayerPosition.CABINET) return;
            ctx.currentPosition = (ctx.currentPosition == PlayerPosition.FRONT_ROOM)
                    ? PlayerPosition.BACK_ROOM : PlayerPosition.FRONT_ROOM;
            inputHandler.updateUIVisibility();
            ctx.officePanel.repaint();
        };
        ctx.btnLookLeft.addActionListener(lookAction);
        ctx.btnLookRight.addActionListener(lookAction);

        ctx.btnDoor.addActionListener(e -> {
            ctx.logEvent("Pintu ini sekarang hanya dikontrol oleh sistem/entitas.");
        });

        ctx.btnPeekKeyhole.addActionListener(e -> {
            Action act = getActionMap().get("peekKeyhole");
            if (act != null) act.actionPerformed(null);
        });

        ctx.btnPeekVent.addActionListener(e -> {
            Action act = getActionMap().get("peekVent");
            if (act != null) act.actionPerformed(null);
        });

        inputHandler.updateUIVisibility();
    }

    // ============================================================
    // SETUP RESPONSIVE LAYOUT
    // ============================================================

    private void setupResponsiveListener() {
        ctx.layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = ctx.layeredPane.getWidth();
                int h = ctx.layeredPane.getHeight();

                ctx.officePanel.setBounds(0, 0, w, h);
                ctx.endScreenPanel.setBounds(0, 0, w, h);
                ctx.lockpickPopupPanel.setBounds(0, 0, w, h);

                int btnW = HitboxConfig.BTN_CENTER_W;
                int btnH = HitboxConfig.BTN_CENTER_H;
                int startX = (w - btnW) / 2;
                int posY = h - btnH - HitboxConfig.BTN_CENTER_MARGIN_BOTTOM;
                ctx.btnDoor.setBounds(startX, posY, btnW, btnH);

                int peekBtnW = 280;
                int peekBtnH = HitboxConfig.BTN_CENTER_H;
                int peekY = posY - peekBtnH - 10;
                if (ctx.btnPeekKeyhole != null) ctx.btnPeekKeyhole.setBounds(w / 2 - peekBtnW - 10, peekY, peekBtnW, peekBtnH);
                if (ctx.btnPeekVent != null) ctx.btnPeekVent.setBounds(w / 2 + 10, peekY, peekBtnW, peekBtnH);

                int ew = HitboxConfig.BTN_EDGE_W;
                int eh = HitboxConfig.BTN_EDGE_H;
                int midY = (h - eh) / 2;
                ctx.btnLookLeft.setBounds(HitboxConfig.BTN_EDGE_MARGIN_SIDE, midY, ew, eh);
                ctx.btnLookRight.setBounds(w - ew - HitboxConfig.BTN_EDGE_MARGIN_SIDE, midY, ew, eh);
            }
        });
    }

    // ============================================================
    // SETUP GAME LOOP
    // ============================================================

    private void setupGameLoop() {
        ctx.gameLoopTimer = new Timer(1200, e -> processGameTick());

        ctx.lockDrainTimer = new Timer(3000, e -> {
            if (ctx.currentState != GameState.GAMEOVER && !ctx.lockpickPopupPanel.isVisible() && ctx.lockBars > 0) {
                ctx.lockBars--;
                ctx.logEvent("⚠️ [PENALTY] Progres gembok menurun (" + ctx.lockBars + "/6 bar).");
            }
        });
    }

    private void processGameTick() {
        if (ctx.currentState == GameState.GAMEOVER || ctx.currentState == GameState.STRUGGLING || ctx.isRetreating)
            return;

        if (ctx.currentState == GameState.HALLWAY) {
            if (Math.random() < 0.1 && !ctx.isFlickering) {
                combatSystem.startFlickerEffect();
            }
            return;
        }

        if (ctx.areEnemiesActive) {
            if (ctx.enemyA.isAtDoor() || ctx.getEnemyAtDoor() == null) {
                if (ctx.currentPosition != PlayerPosition.PEEKING_KEYHOLE || !ctx.enemyA.isAtDoor()) {
                    ctx.enemyA.act();
                }
            }
            if (ctx.enemyB.isAtDoor() || ctx.getEnemyAtDoor() == null) {
                ctx.enemyB.act();
            }

            Enemy attacker = ctx.getEnemyAtDoor();
            if (attacker != null) {
                combatSystem.checkDoorDefense(attacker);
                int p = attacker.getPatienceTimer();
                if (p <= 1) ctx.vignetteIntensity = 0.8f;
                else if (p == 2) ctx.vignetteIntensity = 0.5f;
                else if (p == 3) ctx.vignetteIntensity = 0.2f;
                else ctx.vignetteIntensity = 0f;
            } else {
                ctx.vignetteIntensity = 0f;
            }

            combatSystem.updateDoorVisuals();
        }
    }

    // ============================================================
    // PUBLIC API
    // ============================================================

    public void startGame() {
        this.requestFocusInWindow();
        inputHandler.updateUIVisibility();
        hallwaySystem.startGame();
    }

    // ============================================================
    // RESET GAME
    // ============================================================

    private void resetGame() {
        stopAllTimers();
        AudioManager.stopAllSounds();

        ctx.initGameData();
        ctx.endScreenPanel.setVisible(false);
        ctx.lockpickPopupPanel.setVisible(false);

        ctx.btnDoor.setVisible(true);
        ctx.btnLookLeft.setVisible(true);
        ctx.btnLookRight.setVisible(true);
        ctx.btnDoor.setText("🚪 TUTUP PINTU DEPAN [E]");

        ctx.statusLabel.setText(
                "Objective: Bobol rantai pintu belakang sebelum mereka menangkapmu. (F11 = Maximize)");
        ctx.statusLabel.setForeground(Color.YELLOW);

        combatSystem.updateDoorVisuals();
        inputHandler.updateUIVisibility();
        ctx.gameLoopTimer.start();
        if (ctx.lockDrainTimer != null)
            ctx.lockDrainTimer.start();
    }

    // ============================================================
    // TIMER MANAGEMENT
    // ============================================================

    private void stopAllTimers() {
        if (ctx.quoteTimer != null && ctx.quoteTimer.isRunning()) ctx.quoteTimer.stop();
        if (ctx.struggleTimer != null && ctx.struggleTimer.isRunning()) ctx.struggleTimer.stop();
        if (ctx.lockDrainTimer != null && ctx.lockDrainTimer.isRunning()) ctx.lockDrainTimer.stop();
        if (ctx.gameLoopTimer != null && ctx.gameLoopTimer.isRunning()) ctx.gameLoopTimer.stop();
        if (ctx.flickerTimer != null && ctx.flickerTimer.isRunning()) ctx.flickerTimer.stop();
        if (ctx.typingTimer != null && ctx.typingTimer.isRunning()) ctx.typingTimer.stop();
    }

    @Override
    public void stopAllProcesses() {
        if (ctx.gameLoopTimer != null && ctx.gameLoopTimer.isRunning()) ctx.gameLoopTimer.stop();
        if (ctx.lockDrainTimer != null && ctx.lockDrainTimer.isRunning()) ctx.lockDrainTimer.stop();
        if (ctx.retreatTimer != null && ctx.retreatTimer.isRunning()) ctx.retreatTimer.stop();
        if (ctx.quoteTimer != null && ctx.quoteTimer.isRunning()) ctx.quoteTimer.stop();
        if (ctx.flickerTimer != null && ctx.flickerTimer.isRunning()) ctx.flickerTimer.stop();
    }
}
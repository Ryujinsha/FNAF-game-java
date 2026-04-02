package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.TimeSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class GameGUI extends JPanel { 
    private MainFrame mainFrame; 

    // --- 1. DATA LOGIC GAME ---
    private Player player;
    private EnemyOdd enemyA;
    private EnemyEven enemyB;
    private EnemyRandom enemyC;
    private boolean areEnemiesActive = false;
    private int tickCounter = 0;
    private boolean isGameOver = false;
    
    private JLabel statusLabel;
    
    private PixelButton btnTablet, btnLeftDoor, btnRightDoor, btnLookLeft, btnLookRight;
    private JLayeredPane layeredPane; 
    
    private JPanel officePanel;
    private JPanel tabletOverlayPanel;
    
    // --- 2. SISTEM PERBAIKAN & KEYPAD ---
    private JProgressBar repairProgressBar;
    private JButton btnStartRepair;
    private int repairProgress = 0;
    private boolean isRepairing = false;
    private Timer repairTimer;
    private boolean isKeypadActive = false; 
    
    private JPanel keypadPopupPanel;
    private JLabel keypadDisplayLabel;
    private String secretPin = "";
    private String currentPinInput = "";
    
    // ✨ SISTEM SECOND CHANCE & EASTER EGG PIN
    private boolean hasSecondChance = true;
    private boolean pinRevealed = false;
    private Rectangle hiddenPinFront;
    private Rectangle hiddenPinBack;
    
    private JPanel endScreenPanel;
    private JLabel endTitleLabel;
    private JLabel endMessageLabel;

    private Image leftDoorVisual = null;
    private Image rightDoorVisual = null;

    private Timer gameLoopTimer; 
    private Timer quoteTimer;
    
    private boolean isLookingBack = false;

    public GameGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initGameData();

        setLayout(new BorderLayout());
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        setupUI();
        setupResponsiveListener(); 
        setupGameLoop();
    }

    private void initGameData() {
        this.player = new Player("Night Guard");
        this.enemyA = new EnemyOdd("Epstein", 20);  
        this.enemyB = new EnemyEven("Diddy", 20);   
        this.enemyC = new EnemyRandom("Wowo", 15);  
        this.areEnemiesActive = false;
        this.tickCounter = 0;
        this.isGameOver = false;
        this.repairProgress = 0;
        this.isRepairing = false;
        this.isLookingBack = false;
        this.isKeypadActive = false; 
        
        this.secretPin = String.format("%04d", (int)(Math.random() * 10000));
        this.currentPinInput = "";
        
        // ✨ RESET SECOND CHANCE & GENERATE POSISI PIN RAHASIA ACAK
        this.hasSecondChance = true;
        this.pinRevealed = false;
        
        // Acak posisi di area dinding (1300x900 base resolution)
        int rx1 = (int)(Math.random() * 1000) + 150;
        int ry1 = (int)(Math.random() * 600) + 100;
        hiddenPinFront = new Rectangle(rx1, ry1, 80, 80);

        int rx2 = (int)(Math.random() * 1000) + 150;
        int ry2 = (int)(Math.random() * 600) + 100;
        hiddenPinBack = new Rectangle(rx2, ry2, 80, 80);
        
        System.out.println("🤫 [CHEAT LOG] PIN: " + secretPin + " | Posisi Depan: " + rx1 + "," + ry1 + " | Posisi Belakang: " + rx2 + "," + ry2);
    }

    private void setupUI() {
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.DARK_GRAY);
        statusLabel = new JLabel("Menyiapkan sistem...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 18)); 
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        officePanel = new JPanel() {
            private Image bgBawah, bgAtas, doorLeftImg, doorRightImg, bgBack; 

            {
                URL urlBawah = getClass().getResource("/assets/office_bg.png");
                if (urlBawah != null) bgBawah = new ImageIcon(urlBawah).getImage();

                URL urlAtas = getClass().getResource("/assets/office_front.png");
                if (urlAtas != null) bgAtas = new ImageIcon(urlAtas).getImage();

                URL urlDoorL = getClass().getResource("/assets/door_left.png");
                if (urlDoorL != null) doorLeftImg = new ImageIcon(urlDoorL).getImage();

                URL urlDoorR = getClass().getResource("/assets/door_right.png");
                if (urlDoorR != null) doorRightImg = new ImageIcon(urlDoorR).getImage();
                
                URL urlBack = getClass().getResource("/assets/back_side.png");
                if (urlBack != null) bgBack = new ImageIcon(urlBack).getImage();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (isLookingBack) {
                    if (bgBack != null) g.drawImage(bgBack, 0, 0, getWidth(), getHeight(), this);
                    else { g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(), getHeight()); }
                    return; 
                }

                if (bgBawah != null) g.drawImage(bgBawah, 0, 0, getWidth(), getHeight(), this);
                else { g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(), getHeight()); }

                int doorW = getWidth() / 4; 
                int doorH = (int)(getHeight() * 0.7); 
                int doorY = getHeight() - doorH - 20;

                if (leftDoorVisual != null && !player.isLeftDoorClosed()) g.drawImage(leftDoorVisual, getWidth() / 10, doorY, doorW, doorH, this);
                if (rightDoorVisual != null && !player.isRightDoorClosed()) g.drawImage(rightDoorVisual, getWidth() - doorW - (getWidth() / 10), doorY, doorW, doorH, this);
                if (bgAtas != null) g.drawImage(bgAtas, 0, 0, getWidth(), getHeight(), this);
                if (player.isLeftDoorClosed() && doorLeftImg != null) g.drawImage(doorLeftImg, 0, 0, getWidth(), getHeight(), this);
                if (player.isRightDoorClosed() && doorRightImg != null) g.drawImage(doorRightImg, 0, 0, getWidth(), getHeight(), this);
            }
        };
        layeredPane.add(officePanel, JLayeredPane.DEFAULT_LAYER);

        // Menggabungkan deteksi klik keypad dan klik easter egg
        setupInteractionHits();
        
        setupKeypadPopupUI();
        layeredPane.add(keypadPopupPanel, JLayeredPane.MODAL_LAYER);
        keypadPopupPanel.setVisible(false);

        setupTabletOverlay();
        layeredPane.add(tabletOverlayPanel, JLayeredPane.PALETTE_LAYER);
        tabletOverlayPanel.setVisible(false);

        setupEndScreen();
        layeredPane.add(endScreenPanel, JLayeredPane.POPUP_LAYER);
        endScreenPanel.setVisible(false);

        setupFloatingControls();
    }

    private void setupKeypadPopupUI() {
        keypadPopupPanel = new JPanel();
        keypadPopupPanel.setBackground(new Color(20, 20, 20, 240));
        keypadPopupPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 5));
        keypadPopupPanel.setLayout(new BorderLayout(10, 10));

        keypadDisplayLabel = new JLabel("----", SwingConstants.CENTER);
        keypadDisplayLabel.setFont(new Font("Consolas", Font.BOLD, 48));
        keypadDisplayLabel.setForeground(new Color(150, 200, 255)); 
        keypadDisplayLabel.setBackground(new Color(10, 30, 50));
        keypadDisplayLabel.setOpaque(true);
        keypadDisplayLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        keypadPopupPanel.add(keypadDisplayLabel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(4, 3, 5, 5));
        gridPanel.setOpaque(false);
        String[] buttons = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "CLR", "0", "ENT"};
        
        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Consolas", Font.BOLD, 24));
            btn.setBackground(Color.DARK_GRAY);
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> handleKeypadInput(text));
            gridPanel.add(btn);
        }
        keypadPopupPanel.add(gridPanel, BorderLayout.CENTER);

        JButton btnCloseKeypad = new JButton("TUTUP KEYPAD");
        btnCloseKeypad.setBackground(Color.RED);
        btnCloseKeypad.setForeground(Color.WHITE);
        btnCloseKeypad.addActionListener(e -> keypadPopupPanel.setVisible(false));
        keypadPopupPanel.add(btnCloseKeypad, BorderLayout.SOUTH);
    }

    private void handleKeypadInput(String key) {
        if (isGameOver) return;

        if (key.equals("CLR")) {
            currentPinInput = "";
            keypadDisplayLabel.setText("----");
            keypadDisplayLabel.setForeground(new Color(150, 200, 255));
            return;
        }

        if (key.equals("ENT")) {
            if (currentPinInput.equals(secretPin)) {
                keypadDisplayLabel.setText("ACCESS GRANTED");
                keypadDisplayLabel.setForeground(Color.GREEN);
                endGame("VICTORY", "Pintu terbuka! Kamu berhasil kabur.", Color.GREEN);
            } else {
                keypadDisplayLabel.setText("DENIED");
                keypadDisplayLabel.setForeground(Color.RED);
                
                // Kurangi power
                player.getPower().decreasePower(15); 
                updateStatusLabel();
                
                // ✨ LOGIKA SECOND CHANCE
                if (player.getPower().getCurrentPower() <= 0) {
                    if (hasSecondChance) {
                        hasSecondChance = false;
                        player.getPower().addPower(30); // Berikan 30% daya darurat
                        
                        logEvent("⚠️ [EMERGENCY] Daya habis! Generator darurat menyala (+30% Power).");
                        JOptionPane.showMessageDialog(this, "Sistem Anjlok!\nGenerator cadangan menyala memberi 30% daya.\nKamu harus melakukan START REPAIR ulang!", "WARNING", JOptionPane.WARNING_MESSAGE);
                        
                        // Reset status perbaikan
                        keypadPopupPanel.setVisible(false);
                        isKeypadActive = false;
                        repairProgress = 0;
                        repairProgressBar.setValue(0);
                        btnStartRepair.setText("START REPAIR");
                        btnStartRepair.setEnabled(true);
                        btnStartRepair.setForeground(Color.GREEN);
                        updateStatusLabel();
                    } else {
                        checkWinLoss(); // Jika kesempatan kedua sudah dipakai, mati.
                    }
                } else {
                    logEvent("❌ [KEYPAD] PIN SALAH! Sistem menyedot sisa daya... Power -15%");
                }
                
                Timer resetTimer = new Timer(1000, evt -> {
                    if (!isGameOver) {
                        currentPinInput = "";
                        keypadDisplayLabel.setText("----");
                        keypadDisplayLabel.setForeground(new Color(150, 200, 255));
                    }
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
            return;
        }

        if (currentPinInput.length() < 4 && !keypadDisplayLabel.getText().equals("DENIED")) {
            currentPinInput += key;
            com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/button_click.wav"); 
            
            StringBuilder display = new StringBuilder(currentPinInput);
            while (display.length() < 4) display.append("-");
            keypadDisplayLabel.setText(display.toString());
        }
    }

    // ✨ METHOD BARU: Menangani klik Keypad (Diperbesar) & Klik Area Rahasia
    private void setupInteractionHits() {
        layeredPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isGameOver) return;

                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                int finalX = (e.getX() * 1300) / w; 
                int finalY = (e.getY() * 900) / h; 

                // 1. Cek Hitbox Rahasia PIN
                if (!pinRevealed && !player.isTabletOpen()) {
                    boolean foundFront = !isLookingBack && hiddenPinFront.contains(finalX, finalY);
                    boolean foundBack = isLookingBack && hiddenPinBack.contains(finalX, finalY);
                    
                    if (foundFront || foundBack) {
                        pinRevealed = true;
                        logEvent("🔍 [DISCOVERY] Kamu menemukan coretan tersembunyi! PIN: " + secretPin);
                        com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/light_switch.wav"); // Sfx penemuan
                        JOptionPane.showMessageDialog(layeredPane, "Kamu melihat goresan di dinding...\nAngkanya terlihat seperti: " + secretPin, "Secret Note Found", JOptionPane.INFORMATION_MESSAGE);
                    }
                }

                // 2. Cek Hitbox Keypad (Hitbox diperbesar menjadi 200x250)
                if (isLookingBack && isKeypadActive && !keypadPopupPanel.isVisible()) {
                    Rectangle keypadArea = new Rectangle(750, 300, 200, 250); 
                    
                    if (keypadArea.contains(finalX, finalY)) {
                        logEvent("🔍 [INTERACT] Membuka antarmuka Keypad...");
                        keypadPopupPanel.setVisible(true);
                    }
                }
            }
        });
    }

    private void updateDoorVisuals() {
        Enemy leftEnemy = getEnemyAtDoor("LEFT");
        leftDoorVisual = (leftEnemy != null) ? new ImageIcon(getClass().getResource(getSpritePath(leftEnemy))).getImage() : null;

        Enemy rightEnemy = getEnemyAtDoor("RIGHT");
        rightDoorVisual = (rightEnemy != null) ? new ImageIcon(getClass().getResource(getSpritePath(rightEnemy))).getImage() : null;

        officePanel.repaint();
    }

    private String getSpritePath(Enemy enemy) {
        if (enemy instanceof EnemyOdd) return "/assets/enemies/enemy_a.png";
        if (enemy instanceof EnemyEven) return "/assets/enemies/enemy_b.png";
        if (enemy instanceof EnemyRandom) return "/assets/enemies/enemy_c.png";
        return "/assets/enemies/enemy_a.png";
    }

    private void setupResponsiveListener() {
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                officePanel.setBounds(0, 0, w, h);
                endScreenPanel.setBounds(0, 0, w, h);
                
                int tabW = 924;
                int tabH = 550;
                tabletOverlayPanel.setBounds((w - tabW) / 2, (h - tabH) / 2, tabW, tabH);
                
                int keyW = 350;
                int keyH = 500;
                keypadPopupPanel.setBounds((w - keyW) / 2, (h - keyH) / 2, keyW, keyH);

                int btnW = 250, btnH = 60, gap = 30;
                int startX = (w - (3 * btnW + 2 * gap)) / 2; 
                int posY = h - btnH - 30; 

                btnLeftDoor.setBounds(startX, posY, btnW, btnH);
                btnTablet.setBounds(startX + btnW + gap, posY, btnW, btnH);
                btnRightDoor.setBounds(startX + 2 * (btnW + gap), posY, btnW, btnH);

                int edgeBtnW = 50, edgeBtnH = 150, midY = (h - edgeBtnH) / 2;
                btnLookLeft.setBounds(10, midY, edgeBtnW, edgeBtnH);
                btnLookRight.setBounds(w - 60, midY, edgeBtnW, edgeBtnH);
            }
        });
    }

    private void setupTabletOverlay() {
        tabletOverlayPanel = new JPanel();
        tabletOverlayPanel.setBackground(new Color(10, 20, 10, 230)); 
        tabletOverlayPanel.setLayout(new GridBagLayout());
        tabletOverlayPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("SYSTEM DIAGNOSTIC & REPAIR", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 36));
        titleLabel.setForeground(Color.GREEN);
        tabletOverlayPanel.add(titleLabel, gbc);

        repairProgressBar = new JProgressBar(0, 100);
        repairProgressBar.setValue(0);
        repairProgressBar.setStringPainted(true);
        repairProgressBar.setFont(new Font("Consolas", Font.BOLD, 24));
        repairProgressBar.setForeground(Color.GREEN);
        repairProgressBar.setBackground(Color.DARK_GRAY);
        repairProgressBar.setPreferredSize(new Dimension(600, 50));
        tabletOverlayPanel.add(repairProgressBar, gbc);

        btnStartRepair = new JButton("START REPAIR");
        btnStartRepair.setFont(new Font("Consolas", Font.BOLD, 28));
        btnStartRepair.setBackground(Color.DARK_GRAY);
        btnStartRepair.setForeground(Color.GREEN);
        btnStartRepair.setFocusPainted(false);
        btnStartRepair.addActionListener(e -> {
            if (isGameOver || repairProgress >= 100) return;
            isRepairing = !isRepairing; 
            btnStartRepair.setText(isRepairing ? "PAUSE REPAIR" : "RESUME REPAIR");
        });
        tabletOverlayPanel.add(btnStartRepair, gbc);
    }

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
            if (quoteTimer != null && quoteTimer.isRunning()) quoteTimer.stop();
            com.ryujinsha.system.AudioManager.stopAllSounds();
            mainFrame.showScreen("MENU");
        });

        btnPanel.add(btnRetry);
        btnPanel.add(btnMenu); 
        endScreenPanel.add(btnPanel, gbc); 
    }

    private void setupFloatingControls() {
        btnLeftDoor = new PixelButton("🚪 L-Door [OPEN]");
        btnTablet = new PixelButton("📱 Tablet [OFF]");
        btnRightDoor = new PixelButton("🚪 R-Door [OPEN]");
        
        btnLookLeft = new PixelButton("◀");
        btnLookRight = new PixelButton("▶");

        layeredPane.add(btnLeftDoor, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnTablet, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnRightDoor, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnLookLeft, JLayeredPane.MODAL_LAYER);
        layeredPane.add(btnLookRight, JLayeredPane.MODAL_LAYER);

        java.awt.event.ActionListener lookAction = e -> {
            if (isGameOver || player.isTabletOpen() || keypadPopupPanel.isVisible()) return;
            isLookingBack = !isLookingBack;
            officePanel.repaint();
        };
        btnLookLeft.addActionListener(lookAction);
        btnLookRight.addActionListener(lookAction);

        btnTablet.addActionListener(e -> {
            if (isGameOver || keypadPopupPanel.isVisible()) return;
            
            if (isLookingBack) {
                isLookingBack = false;
                officePanel.repaint();
            }
            
            player.toggleTablet();
            boolean isTabOpen = player.isTabletOpen();
            btnTablet.setText("📱 Tablet [" + (isTabOpen ? "ON" : "OFF") + "]");
            tabletOverlayPanel.setVisible(isTabOpen);
            
            btnLookLeft.setVisible(!isTabOpen);
            btnLookRight.setVisible(!isTabOpen);

            if (isTabOpen && !areEnemiesActive) {
                areEnemiesActive = true;
                logEvent("⚠️ [WARNING] Sesuatu menyadari kehadiranmu...");
            }
        });

        btnLeftDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleLeftDoor();
            btnLeftDoor.setText("🚪 L-Door [" + (player.isLeftDoorClosed() ? "CLOSED" : "OPEN") + "]");
            com.ryujinsha.system.AudioManager.playSound(player.isLeftDoorClosed() ? "/assets/audio/sfx/door_close.wav" : "/assets/audio/sfx/door_open.wav");
            updateDoorVisuals(); 
        });

        btnRightDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleRightDoor();
            btnRightDoor.setText("🚪 R-Door [" + (player.isRightDoorClosed() ? "CLOSED" : "OPEN") + "]");
            com.ryujinsha.system.AudioManager.playSound(player.isRightDoorClosed() ? "/assets/audio/sfx/door_close.wav" : "/assets/audio/sfx/door_open.wav");
            updateDoorVisuals(); 
        });
    }

    private void setupGameLoop() {
        gameLoopTimer = new Timer(2000, e -> processGameTick());
        
        repairTimer = new Timer(100, e -> {
            if (isGameOver) return;
            
            if (player.isTabletOpen() && isRepairing && repairProgress < 100) {
                repairProgress++; 
                repairProgressBar.setValue(repairProgress);
                
                if (repairProgress >= 100) {
                    isRepairing = false;
                    btnStartRepair.setText("SYSTEM REPAIRED");
                    btnStartRepair.setEnabled(false);
                    btnStartRepair.setForeground(Color.GRAY);
                    
                    isKeypadActive = true; 
                    logEvent("✅ [SYSTEM] Perbaikan selesai. KEYPAD BELAKANG AKTIF!");
                }
            }
        });
    }

    private void processGameTick() {
        if (isGameOver) return;

        if (areEnemiesActive) {
            enemyA.act(); enemyB.act(); enemyC.act();
            checkDoorDefense(enemyA); 
            checkDoorDefense(enemyB); 
            checkDoorDefense(enemyC);
            updateDoorVisuals(); 
        }
        
        updateStatusLabel();
        checkWinLoss();
    }

    private void checkDoorDefense(Enemy enemy) {
        if (enemy.isAtDoor()) {
            // ✨ FIX: Mengembalikan peringatan napas agar Anda tidak mati tanpa aba-aba
            if (enemy.getPatienceTimer() == 3) {
                if (enemy.getDoorTarget().equals("LEFT")) {
                    logEvent("🌑 *suara napas berat*... Ada siluet di pintu KIRI.");
                } else if (enemy.getDoorTarget().equals("RIGHT")) {
                    logEvent("🌑 *suara napas berat*... Ada siluet di pintu KANAN.");
                }
            }
            
            boolean isDefended = (enemy.getDoorTarget().equals("LEFT") && player.isLeftDoorClosed()) ||
                                 (enemy.getDoorTarget().equals("RIGHT") && player.isRightDoorClosed());
            
            if (isDefended) {
                logEvent("💥 *BAM BAM BAM* " + enemy.getName() + " memukul pintu!");
                int randomBang = (int)(Math.random() * 3) + 1;
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_bang_" + randomBang + ".wav");
                enemy.retreat(7);
                updateDoorVisuals(); 
            } else if (enemy.getPatienceTimer() <= 0) {
                triggerJumpscare(enemy);
            }
        }
    }

    private void updateStatusLabel() {
        String systemStatus = repairProgress >= 100 ? "REPAIRED (Keypad Online)" : "DAMAGED (" + repairProgress + "%)";
        statusLabel.setText(String.format("Power: %d%% | System: %s", player.getPower().getCurrentPower(), systemStatus));
    }

    private void checkWinLoss() {
        if (player.getPower().isPowerEmpty()) {
            triggerJumpscare(enemyC, true);
        }
    }

    private void triggerJumpscare(Enemy enemy) {
        triggerJumpscare(enemy, false);
    }

    private void triggerJumpscare(Enemy enemy, boolean withDelay) {
        if (isGameOver) return;
        isGameOver = true;
        gameLoopTimer.stop(); 
        tabletOverlayPanel.setVisible(false);
        keypadPopupPanel.setVisible(false);
        
        btnTablet.setVisible(false);
        btnLeftDoor.setVisible(false);
        btnRightDoor.setVisible(false);
        btnLookLeft.setVisible(false);
        btnLookRight.setVisible(false);

        com.ryujinsha.system.AudioManager.stopAllSounds();

        if (withDelay) {
            Timer suspenseTimer = new Timer(3000, e -> executeJumpscareVisuals(enemy));
            suspenseTimer.setRepeats(false);
            suspenseTimer.start();
        } else {
            executeJumpscareVisuals(enemy);
        }
    }

    private void executeJumpscareVisuals(Enemy enemy) {
        com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/jumpscare_scream.wav");
        String imagePath = enemy.getJumpscarePath();
        
        JPanel jumpscarePanel = new JPanel() {
            private Image jsImage;
            {
                setOpaque(false); 
                URL imgUrl = getClass().getResource(imagePath);
                if (imgUrl != null) jsImage = new ImageIcon(imgUrl).getImage();
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (jsImage != null) g.drawImage(jsImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        
        jumpscarePanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        layeredPane.add(jumpscarePanel, JLayeredPane.MODAL_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();

        Timer delayTimer = new Timer(1500, e -> {
            layeredPane.remove(jumpscarePanel); 
            endGame("GAME OVER", "Kamu diterkam oleh " + enemy.getName(), Color.RED, enemy);
        });
        delayTimer.setRepeats(false); 
        delayTimer.start();
    }

    private void endGame(String title, String msg, Color titleColor) {
        endGame(title, msg, titleColor, null);
    }

    private void endGame(String title, String msg, Color titleColor, Enemy killer) {
        if (!isGameOver) isGameOver = true; 
        gameLoopTimer.stop();
        keypadPopupPanel.setVisible(false); 
        
        endTitleLabel.setText(title);
        endTitleLabel.setForeground(titleColor);
        endMessageLabel.setText(msg);
        
        endScreenPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        endScreenPanel.setVisible(true); 
        
        endScreenPanel.revalidate();
        endScreenPanel.repaint();

        if (killer != null && killer.getQuotePath() != null) {
            quoteTimer = new Timer(1500, e -> com.ryujinsha.system.AudioManager.playSound(killer.getQuotePath()));
            quoteTimer.setRepeats(false);
            quoteTimer.start();
        }
    }

    private void resetGame() {
        if (quoteTimer != null && quoteTimer.isRunning()) quoteTimer.stop();
        
        if (repairProgressBar != null) repairProgressBar.setValue(0);
        if (btnStartRepair != null) {
            btnStartRepair.setText("START REPAIR");
            btnStartRepair.setEnabled(true);
            btnStartRepair.setForeground(Color.GREEN);
        }
        com.ryujinsha.system.AudioManager.stopAllSounds();

        initGameData();
        endScreenPanel.setVisible(false);
        tabletOverlayPanel.setVisible(false);
        keypadPopupPanel.setVisible(false);
        leftDoorVisual = null;
        rightDoorVisual = null;
        
        btnTablet.setVisible(true);
        btnLeftDoor.setVisible(true);
        btnRightDoor.setVisible(true);
        btnLookLeft.setVisible(true);
        btnLookRight.setVisible(true);
        
        btnTablet.setText("📱 Tablet [OFF]");
        btnLeftDoor.setText("🚪 L-Door [OPEN]");
        btnRightDoor.setText("🚪 R-Door [OPEN]");
        
        currentPinInput = "";
        keypadDisplayLabel.setText("----");
        keypadDisplayLabel.setForeground(new Color(150, 200, 255));

        updateDoorVisuals();
        updateStatusLabel();
        
        gameLoopTimer.start();
        if (repairTimer != null) repairTimer.start(); 
    }

    public void startGame() {
        updateStatusLabel();
        gameLoopTimer.start();
        if (repairTimer != null) repairTimer.start(); 
    }

    private Enemy getEnemyAtDoor(String doorTarget) {
        if (enemyA.isAtDoor() && enemyA.getDoorTarget().equals(doorTarget)) return enemyA;
        if (enemyB.isAtDoor() && enemyB.getDoorTarget().equals(doorTarget)) return enemyB;
        if (enemyC.isAtDoor() && enemyC.getDoorTarget().equals(doorTarget)) return enemyC;
        return null; 
    }

    private void logEvent(String message) {
        System.out.println(message); 
    }
}
package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.ImageLoader;
import com.ryujinsha.system.TimeSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class GameGUI extends JPanel { // ✨ REFACTOR 1: Berubah menjadi JPanel
    private MainFrame mainFrame; // ✨ REFACTOR 2: Referensi ke window utama

    // --- 1. DATA LOGIC GAME ---
    private Player player;
    private EnemyOdd enemyA;
    private EnemyEven enemyB;
    private EnemyRandom enemyC;
    private TimeSystem timeSystem;
    private boolean areEnemiesActive = false;
    private int tickCounter = 0;
    private boolean isGameOver = false;
    private int jokeClickCount = 0;

    // --- 2. KOMPONEN UI UTAMA ---
    private JLabel statusLabel;
    private JButton btnTablet, btnLeftDoor, btnRightDoor, btnLeftLight, btnRightLight;
    private JLayeredPane layeredPane; 
    
    // --- 3. KOMPONEN VISUAL & OVERLAY ---
    private JPanel officePanel;
    private JPanel tabletOverlayPanel;
    private JLabel cameraFeedLabel; 
    private int currentCameraView = 7; 
    private JPanel endScreenPanel;
    private JLabel endTitleLabel;
    private JLabel endMessageLabel;

    // ✨ GAMBAR HANTU DI PINTU
    private Image leftDoorVisual = null;
    private Image rightDoorVisual = null;

    // --- 4. TIMER PENGGERAK ---
    private Timer gameLoopTimer; 
    private Timer quoteTimer;

    // ✨ REFACTOR 3: Konstruktor menerima MainFrame
    public GameGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initGameData();

        // setSize, setTitle, setDefaultCloseOperation dihapus karena diurus oleh MainFrame
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
        this.timeSystem = new TimeSystem();
        this.areEnemiesActive = false;
        this.tickCounter = 0;
        this.isGameOver = false;
        this.jokeClickCount = 0; 
    }

    private void setupUI() {
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.DARK_GRAY);
        statusLabel = new JLabel("Menyiapkan sistem...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        officePanel = new JPanel() {
            private Image bgBawah; 
            private Image bgAtas;  
            private Image doorLeftImg;  
            private Image doorRightImg; 

            {
                URL urlBawah = getClass().getResource("/assets/office_bg.jpg");
                if (urlBawah != null) bgBawah = new ImageIcon(urlBawah).getImage();

                URL urlAtas = getClass().getResource("/assets/office_front.png");
                if (urlAtas != null) bgAtas = new ImageIcon(urlAtas).getImage();

                URL urlDoorL = getClass().getResource("/assets/door_left.png");
                if (urlDoorL != null) doorLeftImg = new ImageIcon(urlDoorL).getImage();

                URL urlDoorR = getClass().getResource("/assets/door_right.png");
                if (urlDoorR != null) doorRightImg = new ImageIcon(urlDoorR).getImage();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (bgBawah != null) {
                    g.drawImage(bgBawah, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                int doorW = getWidth() / 4; 
                int doorH = (int)(getHeight() * 0.7); 
                int doorY = getHeight() - doorH - 20;

                if (leftDoorVisual != null && !player.isLeftDoorClosed()) {
                    int leftX = getWidth() / 10; 
                    g.drawImage(leftDoorVisual, leftX, doorY, doorW, doorH, this);
                }

                if (rightDoorVisual != null && !player.isRightDoorClosed()) {
                    int rightX = getWidth() - doorW - (getWidth() / 10);
                    g.drawImage(rightDoorVisual, rightX, doorY, doorW, doorH, this);
                }

                if (bgAtas != null) {
                    g.drawImage(bgAtas, 0, 0, getWidth(), getHeight(), this);
                }

                if (player.isLeftDoorClosed() && doorLeftImg != null) {
                    g.drawImage(doorLeftImg, 0, 0, getWidth(), getHeight(), this);
                }

                if (player.isRightDoorClosed() && doorRightImg != null) {
                    g.drawImage(doorRightImg, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        layeredPane.add(officePanel, JLayeredPane.DEFAULT_LAYER);

        setupTabletOverlay();
        layeredPane.add(tabletOverlayPanel, JLayeredPane.PALETTE_LAYER);
        tabletOverlayPanel.setVisible(false);

        setupEndScreen();
        layeredPane.add(endScreenPanel, JLayeredPane.POPUP_LAYER);
        endScreenPanel.setVisible(false);

        setupBottomControls();
    }

    private void updateDoorVisuals() {
        Enemy leftEnemy = getEnemyAtDoor("LEFT");
        if (leftEnemy != null) {
            String path = player.isLeftLightOn() ? getSpritePath(leftEnemy) : "/assets/enemies/silhouette.png";
            URL url = getClass().getResource(path);
            leftDoorVisual = (url != null) ? new ImageIcon(url).getImage() : null;
        } else {
            leftDoorVisual = null;
        }

        Enemy rightEnemy = getEnemyAtDoor("RIGHT");
        if (rightEnemy != null) {
            String path = player.isRightLightOn() ? getSpritePath(rightEnemy) : "/assets/enemies/silhouette.png";
            URL url = getClass().getResource(path);
            rightDoorVisual = (url != null) ? new ImageIcon(url).getImage() : null;
        } else {
            rightDoorVisual = null;
        }

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
            }
        });
    }

    private void setupTabletOverlay() {
        tabletOverlayPanel = new JPanel();
        tabletOverlayPanel.setBackground(new Color(0, 0, 0, 200)); 
        tabletOverlayPanel.setLayout(new BorderLayout());
        tabletOverlayPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 2));
        cameraFeedLabel = new JLabel();
        cameraFeedLabel.setHorizontalAlignment(JLabel.CENTER);
        updateCameraFeed(currentCameraView); 
        tabletOverlayPanel.add(cameraFeedLabel, BorderLayout.CENTER);
        JPanel mapPanel = new JPanel();
        mapPanel.setPreferredSize(new Dimension(250, 550));
        mapPanel.setBackground(Color.BLACK);
        mapPanel.setLayout(new GridLayout(4, 2, 5, 5)); 
        for (int i = 1; i <= 7; i++) {
            int camId = i;
            JButton btnCam = new JButton("CAM " + i);
            btnCam.setBackground(Color.DARK_GRAY);
            btnCam.setForeground(Color.GREEN);
            btnCam.addActionListener(e -> {
                currentCameraView = camId;
                updateCameraFeed(currentCameraView);
            });
            mapPanel.add(btnCam);
        }
        JButton btnJoke = new JButton("DO NOT PUSH"); 
        btnJoke.setBackground(Color.DARK_GRAY);
        btnJoke.setForeground(Color.YELLOW); 
        btnJoke.setFocusPainted(false);
        btnJoke.addActionListener(e -> {
            if (isGameOver) return; 

            jokeClickCount++; 
            
            if (jokeClickCount > 3) {
                logEvent("🤡 [FATAL ERROR] Seseorang tidak suka kau bermain-main...");
                triggerJumpscare(enemyC); 
            } else {
                logEvent("🤡 [SYSTEM] Memutar audio rahasia... (" + jokeClickCount + "/3)");
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/joke_sound.wav");
            }
        });
        mapPanel.add(btnJoke);
        mapPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GREEN), "MAP", 0, 0, null, Color.GREEN));
        tabletOverlayPanel.add(mapPanel, BorderLayout.EAST);
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
        JButton btnQuit = new JButton("QUIT GAME");
        btnQuit.setFont(new Font("Consolas", Font.BOLD, 20));
        btnQuit.setBackground(Color.DARK_GRAY);
        btnQuit.setForeground(Color.RED);
        btnQuit.addActionListener(e -> System.exit(0));
        
        JButton btnMenu = new JButton("MAIN MENU");
        btnMenu.setFont(new Font("Consolas", Font.BOLD, 20));
        btnMenu.setBackground(Color.DARK_GRAY);
        btnMenu.setForeground(Color.WHITE);
        btnMenu.addActionListener(e -> {
            if (quoteTimer != null && quoteTimer.isRunning()) {
                quoteTimer.stop();
            }
            com.ryujinsha.system.AudioManager.stopAllSounds();
            
            // ✨ REFACTOR 4: Memanggil MainFrame untuk transisi ke Menu Utama
            mainFrame.showScreen("MENU");
        });

        btnPanel.removeAll(); 
        btnPanel.add(btnRetry);
        btnPanel.add(btnMenu); 
        
        endScreenPanel.add(btnPanel, gbc); 
    }

    private void setupBottomControls() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.DARK_GRAY);
        bottomPanel.setLayout(new GridLayout(1, 5, 5, 0)); 
        
        btnLeftLight = new JButton("💡 L-Light [OFF]");
        btnLeftDoor = new JButton("🚪 L-Door [OPEN]");
        btnTablet = new JButton("📱 Tablet [OFF]");
        btnRightDoor = new JButton("🚪 R-Door [OPEN]");
        btnRightLight = new JButton("💡 R-Light [OFF]");

        bottomPanel.add(btnLeftLight);
        bottomPanel.add(btnLeftDoor);
        bottomPanel.add(btnTablet);
        bottomPanel.add(btnRightDoor);
        bottomPanel.add(btnRightLight);
        add(bottomPanel, BorderLayout.SOUTH);

        btnTablet.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleTablet();
            btnTablet.setText("📱 Tablet [" + (player.isTabletOpen() ? "ON" : "OFF") + "]");
            tabletOverlayPanel.setVisible(player.isTabletOpen());
            if(player.isTabletOpen()) updateCameraFeed(currentCameraView);
            if (player.isTabletOpen() && !areEnemiesActive) {
                areEnemiesActive = true;
                logEvent("⚠️ [WARNING] Sistem menyala. Sesuatu menyadari kehadiranmu...");
            }
        });

        btnLeftDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleLeftDoor();
            btnLeftDoor.setText("🚪 L-Door [" + (player.isLeftDoorClosed() ? "CLOSED" : "OPEN") + "]");
            
            if (player.isLeftDoorClosed()) {
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_close.wav");
            } else {
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_open.wav");
            }
            updateDoorVisuals(); 
        });

        btnRightDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleRightDoor();
            btnRightDoor.setText("🚪 R-Door [" + (player.isRightDoorClosed() ? "CLOSED" : "OPEN") + "]");
            
            if (player.isRightDoorClosed()) {
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_close.wav");
            } else {
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_open.wav");
            }
            updateDoorVisuals(); 
        });

        btnLeftLight.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleLeftLight();
            btnLeftLight.setText("💡 L-Light [" + (player.isLeftLightOn() ? "ON" : "OFF") + "]");
            com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/light_switch.wav");
            checkVisibility("LEFT"); 
            updateDoorVisuals(); 
        });
    }

    private void updateCameraFeed(int camId) {
        String path = "/assets/rooms/room_" + camId + ".png";
        ImageIcon feedIcon = ImageLoader.loadScaledIcon(path, 674, 550); 
        if (feedIcon != null) {
            cameraFeedLabel.setIcon(feedIcon);
            cameraFeedLabel.setText(""); 
        } else {
            cameraFeedLabel.setIcon(null);
            cameraFeedLabel.setText("VIDEO LOSS - CAM " + camId);
            cameraFeedLabel.setForeground(Color.RED);
        }
    }

    private void setupGameLoop() {
        gameLoopTimer = new Timer(2000, e -> processGameTick());
    }

    private void processGameTick() {
        if (isGameOver) return;

        tickCounter++;
        if (tickCounter >= 10) {
            timeSystem.advanceTime();
            tickCounter = 0;
            logEvent("🔔 TENG TONG... Jam menunjukkan pukul " + timeSystem.getFormattedTime());
        }
        
        player.applyStress();

        boolean isIdle = !player.isTabletOpen() && 
                         !player.isLeftDoorClosed() && 
                         !player.isRightDoorClosed() && 
                         !player.isLeftLightOn() && 
                         !player.isRightLightOn();
                         
        if (isIdle) {
            player.getSanity().recoverSanity(1);
            logEvent("🧘‍♂️ [RELAX] Bernapas dalam gelap... Sanity +1%");
        }
        
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
            if (enemy.getPatienceTimer() == 3) {
                if (enemy.getDoorTarget().equals("LEFT") && !player.isLeftLightOn()) {
                    logEvent("🌑 [DARK] *suara napas berat*... Ada siluet di pintu KIRI.");
                } else if (enemy.getDoorTarget().equals("RIGHT") && !player.isRightLightOn()) {
                    logEvent("🌑 [DARK] *suara napas berat*... Ada siluet di pintu KANAN.");
                }
            }
            boolean isDefended = (enemy.getDoorTarget().equals("LEFT") && player.isLeftDoorClosed()) ||
                                 (enemy.getDoorTarget().equals("RIGHT") && player.isRightDoorClosed());
            
            if (isDefended) {
                logEvent("💥 *BAM BAM BAM* " + enemy.getName() + " memukul pintu!");
                int randomBang = (int)(Math.random() * 3) + 1;
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_bang_" + randomBang + ".wav");
                
                player.getSanity().dropSanity(10);
                enemy.retreat(7);
                updateDoorVisuals(); 
            } else if (enemy.getPatienceTimer() <= 0) {
                triggerJumpscare(enemy);
            }
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText(String.format("Time: %s | Power: %d%% | Sanity: %d%% | Monitoring: CAM %d", 
            timeSystem.getFormattedTime(), player.getPower().getCurrentPower(), 
            player.getSanity().getSanityLevel(), currentCameraView));
    }

    private void checkWinLoss() {
        if (timeSystem.isMorning()) {
            endGame("VICTORY", "06:00 AM. Matahari terbit. Kamu selamat malam ini.", Color.GREEN);
        } else if (player.getSanity().isInsane() || player.getPower().isPowerEmpty()) {
            // ✨ MODIFIKASI: Panggil Wowo (enemyC) dengan jeda waktu yang menegangkan
            triggerJumpscare(enemyC, true);
        }
    }

    // Method original untuk menjaga kompatibilitas dengan Pintu dan Easter Egg (tanpa delay)
    private void triggerJumpscare(Enemy enemy) {
        triggerJumpscare(enemy, false);
    }

    // ✨ MODIFIKASI: Method baru yang mendukung delay dan penyembunyian UI
    private void triggerJumpscare(Enemy enemy, boolean withDelay) {
        if (isGameOver) return;
        isGameOver = true;
        gameLoopTimer.stop(); 
        tabletOverlayPanel.setVisible(false);
        
        // 1. Sembunyikan seluruh kontrol pemain untuk menciptakan keputusasaan
        btnTablet.setVisible(false);
        btnLeftDoor.setVisible(false);
        btnRightDoor.setVisible(false);
        btnLeftLight.setVisible(false);
        btnRightLight.setVisible(false);

        // 2. Hentikan semua suara latar
        com.ryujinsha.system.AudioManager.stopAllSounds();

        if (withDelay) {
            logEvent("🔌 [SYSTEM FAILURE] Sistem mati total. Kegelapan dan keheningan menyelimuti...");
            // Timer suspense selama 3 detik (3000ms) sebelum Wowo menerkam
            Timer suspenseTimer = new Timer(3000, e -> {
                executeJumpscareVisuals(enemy);
            });
            suspenseTimer.setRepeats(false);
            suspenseTimer.start();
        } else {
            // Langsung eksekusi untuk kasus gagal menahan pintu
            executeJumpscareVisuals(enemy);
        }
    }

    // ✨ METODE BARU: Menangani murni perenderan gambar dan suara jumpscare
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
        endTitleLabel.setText(title);
        endTitleLabel.setForeground(titleColor);
        endMessageLabel.setText(msg);
        
        endScreenPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        endScreenPanel.setVisible(true); 
        
        // Memaksa Swing menyusun ulang layout tombol agar tidak hilang
        endScreenPanel.revalidate();
        endScreenPanel.repaint();

        layeredPane.revalidate();
        layeredPane.repaint();

        if (killer != null && killer.getQuotePath() != null) {
            System.out.println("[DEBUG-AUDIO] Menyiapkan Timer Quote untuk path: " + killer.getQuotePath());
            
            quoteTimer = new Timer(1500, e -> {
                System.out.println("[DEBUG-AUDIO] Memutar suara quote SEKARANG!");
                com.ryujinsha.system.AudioManager.playSound(killer.getQuotePath());
            });
            quoteTimer.setRepeats(false);
            quoteTimer.start();
        } else {
            System.out.println("[DEBUG-AUDIO] Gagal memutar quote. Killer: " + killer + " | Path: " + (killer != null ? killer.getQuotePath() : "null"));
        }
    }

    private void resetGame() {
        if (quoteTimer != null && quoteTimer.isRunning()) {
            quoteTimer.stop();
        }
        com.ryujinsha.system.AudioManager.stopAllSounds();

        logEvent("\n--- REBOOTING SYSTEM ---");
        initGameData();
        endScreenPanel.setVisible(false);
        tabletOverlayPanel.setVisible(false);
        leftDoorVisual = null;
        rightDoorVisual = null;
        
        btnTablet.setText("📱 Tablet [OFF]");
        btnLeftDoor.setText("🚪 L-Door [OPEN]");
        btnRightDoor.setText("🚪 R-Door [OPEN]");
        btnLeftLight.setText("💡 L-Light [OFF]");
        btnRightLight.setText("💡 R-Light [OFF]");

        currentCameraView = 7;
        updateCameraFeed(currentCameraView);
        updateDoorVisuals();
        updateStatusLabel();
        
        logEvent("Sistem online. Malam pertama dimulai ulang.");
        gameLoopTimer.start();
    }

    public void startGame() {
        // ✨ REFACTOR 5: setVisible(true) dihapus, karena CardLayout menanganinya
        updateStatusLabel();
        logEvent("Sistem online. Malam pertama dimulai.");
        gameLoopTimer.start();
    }

    private void logEvent(String message) {
        System.out.println(message); 
    }

    private Enemy getEnemyAtDoor(String doorTarget) {
        if (enemyA.isAtDoor() && enemyA.getDoorTarget().equals(doorTarget)) return enemyA;
        if (enemyB.isAtDoor() && enemyB.getDoorTarget().equals(doorTarget)) return enemyB;
        if (enemyC.isAtDoor() && enemyC.getDoorTarget().equals(doorTarget)) return enemyC;
        return null; 
    }

    private void checkVisibility(String doorTarget) {
        Enemy enemy = getEnemyAtDoor(doorTarget);
        boolean isLightOn = doorTarget.equals("LEFT") ? player.isLeftLightOn() : player.isRightLightOn();

        if (enemy != null) {
            if (isLightOn) {
                logEvent("🔦 [LIGHT ON] TERLIHAT JELAS! " + enemy.getName() + " menatapmu dari pintu " + doorTarget + "!");
            } else {
                logEvent("🌑 [DARK] Ada SILUET gelap berdiri di pintu " + doorTarget + "...");
            }
        } else if (isLightOn) {
            logEvent("🔦 [LIGHT ON] Lorong " + doorTarget + " aman.");
        }
    }
}
package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.ImageLoader;
import com.ryujinsha.system.TimeSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;

public class GameGUI extends JFrame {
    // --- 1. DATA LOGIC GAME ---
    private Player player;
    private EnemyOdd enemyA;
    private EnemyEven enemyB;
    private EnemyRandom enemyC;
    private TimeSystem timeSystem;
    private boolean areEnemiesActive = false;
    private int tickCounter = 0;
    private boolean isGameOver = false;

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

    // ✨ GAMBAR HANTU DI PINTU (BARU)
    private Image leftDoorVisual = null;
    private Image rightDoorVisual = null;

    // --- 4. TIMER PENGGERAK ---
    private Timer gameLoopTimer; 
    private Timer quoteTimer;

    public GameGUI() {
        initGameData();

        setTitle("Night Shift Survival - GUI Build v1.3 (Visual Doors)");
        setSize(1300, 900); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        setupUI();
        setupResponsiveListener(); 
        setupGameLoop();
    }

    private void initGameData() {
        this.player = new Player("Night Guard");
        this.enemyA = new EnemyOdd("The Twin (Odd)", 20);
        this.enemyB = new EnemyEven("The Twin (Even)", 20);
        this.enemyC = new EnemyRandom("The Shadow", 15);
        this.timeSystem = new TimeSystem();
        this.areEnemiesActive = false;
        this.tickCounter = 0;
        this.isGameOver = false;
    }

    private void setupUI() {
        JPanel topPanel = new JPanel();
        topPanel.setBackground(Color.DARK_GRAY);
        statusLabel = new JLabel("Menyiapkan sistem...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        // ✨ UPDATE: Office Panel sekarang bisa merender Hantu di pintu!
        // ✨ UPDATE: Office Panel dengan Layering Background
        // ✨ UPDATE: Office Panel dengan Layering Background, Hantu, dan Pintu Animasi
        officePanel = new JPanel() {
            private Image bgBawah; // Layer 1: Lorong luar
            private Image bgAtas;  // Layer 3: Dinding ruangan
            private Image doorLeftImg;  // Layer 4: Pintu Kiri
            private Image doorRightImg; // Layer 4: Pintu Kanan

            {
                // Load semua aset visual
                URL urlBawah = getClass().getResource("/assets/office_bg.jpg");
                if (urlBawah != null) bgBawah = new ImageIcon(urlBawah).getImage();

                URL urlAtas = getClass().getResource("/assets/office_front.png");
                if (urlAtas != null) bgAtas = new ImageIcon(urlAtas).getImage();

                // Load aset Pintu (Wajib transparan selain pintunya)
                URL urlDoorL = getClass().getResource("/assets/door_left.png");
                if (urlDoorL != null) doorLeftImg = new ImageIcon(urlDoorL).getImage();

                URL urlDoorR = getClass().getResource("/assets/door_right.png");
                if (urlDoorR != null) doorRightImg = new ImageIcon(urlDoorR).getImage();
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // --- URUTAN LUKISAN (Z-INDEX) SANGAT KRUSIAL ---

                // LAYER 1: Gambar Background Lorong (Paling dasar)
                if (bgBawah != null) {
                    g.drawImage(bgBawah, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }

                // LAYER 2: Gambar Hantu (Hanya dilukis JIKA PINTU TERBUKA)
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

                // LAYER 3: Gambar Dinding Office (Menutupi hantu yang badannya kelebaran)
                if (bgAtas != null) {
                    g.drawImage(bgAtas, 0, 0, getWidth(), getHeight(), this);
                }

                // LAYER 4: PINTU BESI (Menutupi segalanya di lorong)
                // Jika isLeftDoorClosed() bernilai true, lukis pintu kirinya!
                if (player.isLeftDoorClosed() && doorLeftImg != null) {
                    g.drawImage(doorLeftImg, 0, 0, getWidth(), getHeight(), this);
                }

                // Jika isRightDoorClosed() bernilai true, lukis pintu kanannya!
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

    // ✨ METHOD BARU: Logika penentuan gambar pintu
    private void updateDoorVisuals() {
        // Cek Pintu Kiri
        Enemy leftEnemy = getEnemyAtDoor("LEFT");
        if (leftEnemy != null) {
            String path = player.isLeftLightOn() ? getSpritePath(leftEnemy) : "/assets/enemies/silhouette.png";
            URL url = getClass().getResource(path);
            leftDoorVisual = (url != null) ? new ImageIcon(url).getImage() : null;
        } else {
            leftDoorVisual = null;
        }

        // Cek Pintu Kanan
        Enemy rightEnemy = getEnemyAtDoor("RIGHT");
        if (rightEnemy != null) {
            String path = player.isRightLightOn() ? getSpritePath(rightEnemy) : "/assets/enemies/silhouette.png";
            URL url = getClass().getResource(path);
            rightDoorVisual = (url != null) ? new ImageIcon(url).getImage() : null;
        } else {
            rightDoorVisual = null;
        }

        // Paksa UI menggambar ulang layarnya
        officePanel.repaint();
    }

    // Helper untuk menentukan path gambar berdasarkan hantu yang datang
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

    // ... [setupTabletOverlay dan setupEndScreen SAMA seperti v1.2] ...
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
        btnPanel.add(btnRetry);
        JButton btnMenu = new JButton("MAIN MENU");
        btnMenu.setFont(new Font("Consolas", Font.BOLD, 20));
        btnMenu.setBackground(Color.DARK_GRAY);
        btnMenu.setForeground(Color.WHITE);
        btnMenu.addActionListener(e -> {
            // 1. Hentikan semua suara yang mungkin masih berjalan (termasuk timer quote)
            if (quoteTimer != null && quoteTimer.isRunning()) {
                quoteTimer.stop();
            }
            com.ryujinsha.system.AudioManager.stopAllSounds();
            
            // 2. Tutup window GameGUI saat ini
            this.dispose(); 
            
            // 3. Kembalikan pemain ke Main Menu
            SwingUtilities.invokeLater(() -> {
                MainMenuGUI menu = new MainMenuGUI();
                menu.setVisible(true);
            });
        });

        btnPanel.add(btnRetry);
        btnPanel.add(btnMenu); // Masukkan tombol menu ke panel
        // ...
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

        // ✨ UPDATE: Trigger visual saat tombol ditekan
        btnLeftDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleLeftDoor();
            btnLeftDoor.setText("🚪 L-Door [" + (player.isLeftDoorClosed() ? "CLOSED" : "OPEN") + "]");
            updateDoorVisuals(); // Refresh gambar pintu
        });

        btnRightDoor.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleRightDoor();
            btnRightDoor.setText("🚪 R-Door [" + (player.isRightDoorClosed() ? "CLOSED" : "OPEN") + "]");
            updateDoorVisuals(); 
        });

        btnLeftLight.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleLeftLight();
            btnLeftLight.setText("💡 L-Light [" + (player.isLeftLightOn() ? "ON" : "OFF") + "]");
            checkVisibility("LEFT"); 
            updateDoorVisuals(); 
        });

        btnRightLight.addActionListener(e -> {
            if (isGameOver) return;
            player.toggleRightLight();
            btnRightLight.setText("💡 R-Light [" + (player.isRightLightOn() ? "ON" : "OFF") + "]");
            checkVisibility("RIGHT");
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
        
        if (areEnemiesActive) {
            enemyA.act(); enemyB.act(); enemyC.act();
            checkDoorDefense(enemyA); 
            checkDoorDefense(enemyB); 
            checkDoorDefense(enemyC);
            updateDoorVisuals(); // ✨ UPDATE VISUAL SETIAP MUSUH BERGERAK
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
                
                // ✨ AUDIO TRIGGER: Gedoran Pintu Acak (1-3)
                int randomBang = (int)(Math.random() * 3) + 1;
                com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_bang_" + randomBang + ".wav");
                
                player.getSanity().dropSanity(10);
                enemy.retreat(7);
                updateDoorVisuals(); // Hilangkan gambar jika hantu mundur
            }else if (enemy.getPatienceTimer() <= 0) {
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
            endGame("GAME OVER", "Kewarasan/Listrik habis. Kegelapan menelanmu.", Color.RED);
        }
    }

    private void triggerJumpscare(Enemy enemy) {
        if (isGameOver) return;
        isGameOver = true;
        gameLoopTimer.stop(); 
        tabletOverlayPanel.setVisible(false);
        player.toggleTablet(); 
        btnTablet.setText("📱 Tablet [OFF]");
        com.ryujinsha.system.AudioManager.stopAllSounds();
        com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/jumpscare_scream.wav");
        String imagePath = enemy.getJumpscarePath();
        
        JPanel jumpscarePanel = new JPanel() {
            private Image jsImage;
            {
                setOpaque(false); // ✨ KUNCI UTAMA FIX BACKGROUND PUTIH!
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
            endGame("GAME OVER", "Kamu diterkam oleh " + enemy.getName(), Color.RED);
        });
        delayTimer.setRepeats(false); 
        delayTimer.start();
    }

// Method original untuk kondisi menang atau mati tanpa jumpscare
    private void endGame(String title, String msg, Color titleColor) {
        endGame(title, msg, titleColor, null);
    }

    // Method overload baru untuk menangani audio jumpscare
    private void endGame(String title, String msg, Color titleColor, Enemy killer) {
        if (!isGameOver) isGameOver = true; 
        gameLoopTimer.stop();
        endTitleLabel.setText(title);
        endTitleLabel.setForeground(titleColor);
        endMessageLabel.setText(msg);
        endScreenPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
        endScreenPanel.setVisible(true); 
        layeredPane.revalidate();
        layeredPane.repaint();

        // ✨ AUDIO TRIGGER: Putar quote musuh setelah hening 1.5 detik
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

        initGameData();
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
        setVisible(true);
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
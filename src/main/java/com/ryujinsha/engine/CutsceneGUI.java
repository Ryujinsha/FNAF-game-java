package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class CutsceneGUI extends JPanel {
    private MainFrame mainFrame;
    private JLayeredPane layeredPane; 

    private JTextArea textArea;
    private String fullText;
    private int charIndex = 0;
    private Timer typewriterTimer;
    private boolean isTextFinished = false;

    private int playerX = -50; 
    private int playerY = 220;
    private Image currentPlayerImg;
    private Image imgFront, imgBack, imgLeft, imgRight;
    private Image bgRoad1, bgRoad2, currentBg;
    
    private Timer animationTimer;
    private int animPhase = 0; 
    private PixelButton btnOpenDoor; 
    private PixelButton btnSkip; // ✨ TOMBOL SKIP BARU
    private boolean[] dialogueTriggered = new boolean[5]; 

    public CutsceneGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(Color.BLACK);
        setLayout(new BorderLayout());

        loadAssets();
        
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        JPanel animPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                
                if (currentBg != null) {
                    g2d.drawImage(currentBg, 0, 0, getWidth(), getHeight(), this);
                }
                if (currentPlayerImg != null) {
                    g2d.drawImage(currentPlayerImg, playerX, playerY, 64, 64, this);
                }
            }
        };
        animPanel.setBounds(0, 0, 1300, 500);
        animPanel.setOpaque(false);
        layeredPane.add(animPanel, JLayeredPane.DEFAULT_LAYER);

        // ✨ IMPLEMENTASI TOMBOL SKIP
        btnSkip = new PixelButton("SKIP >>");
        btnSkip.setBounds(1100, 20, 150, 40); // Sudut kanan atas
        btnSkip.addActionListener(e -> {
            typewriterTimer.stop();
            animationTimer.stop();
            mainFrame.showScreen("GAME");
        });
        layeredPane.add(btnSkip, JLayeredPane.PALETTE_LAYER);

        btnOpenDoor = new PixelButton("KLIK UNTUK BUKA PINTU");
        btnOpenDoor.setBounds(500, 350, 300, 60); 
        btnOpenDoor.setVisible(false); 
        btnOpenDoor.addActionListener(e -> {
            com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/door_open.wav");
            mainFrame.showScreen("GAME");
        });
        layeredPane.add(btnOpenDoor, JLayeredPane.PALETTE_LAYER);

        textArea = new JTextArea();
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 24));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(20, 150, 20, 150));
        
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBackground(Color.BLACK);
        textPanel.add(textArea, BorderLayout.CENTER);
        textPanel.setPreferredSize(new Dimension(1300, 400));
        add(textPanel, BorderLayout.SOUTH);

        typewriterTimer = new Timer(50, e -> processTypewriterEffect());
        animationTimer = new Timer(30, e -> {
            updateAnimation();
            animPanel.repaint();
        });

        setupInteraction();

        currentBg = bgRoad1;
        setDialogue("Tahun 2026...\nMereka bilang ini hanya pekerjaan mudah.");
        animationTimer.start();
    }

    private void loadAssets() {
        imgFront = loadSafeImage("/assets/cutscenes/char_1_front.png");
        imgBack  = loadSafeImage("/assets/cutscenes/char_1_back.png");
        imgLeft  = loadSafeImage("/assets/cutscenes/char_1_left.png");
        imgRight = loadSafeImage("/assets/cutscenes/char_1_right.png");
        bgRoad1  = loadSafeImage("/assets/cutscenes/road_1.png");
        bgRoad2  = loadSafeImage("/assets/cutscenes/road_2.png");
        currentPlayerImg = imgRight; 
    }

    private void updateAnimation() {
        if (animPhase == 0) {
            playerX += 2;
            currentPlayerImg = imgRight;
            if (playerX == 400 && !dialogueTriggered[0]) {
                setDialogue("Rumah tua megah yang selalu jadi perbincangan warga...\nKatanya sudah kosong belasan tahun.");
                dialogueTriggered[0] = true;
            }
            if (playerX >= 1150) animPhase = 1;
        } else if (animPhase == 1) {
            playerY += 2;
            currentPlayerImg = imgFront;
            if (playerY >= 550) {
                animPhase = 2; currentBg = bgRoad2;
                playerX = 550; playerY = 550; 
            }
        } else if (animPhase == 2) {
            playerY -= 2;
            currentPlayerImg = imgBack;
            if (playerY == 400 && !dialogueTriggered[1]) {
                setDialogue("Pantas saja tidak ada yang berani mendekat.\nUdaranya terasa... sangat tidak wajar.");
                dialogueTriggered[1] = true;
            }
            if (playerY <= 250) animPhase = 3;
        } else if (animPhase == 3) {
            playerX += 2;
            currentPlayerImg = imgRight;
            if (playerX >= 1150) animPhase = 4;
        } else if (animPhase == 4) {
            playerY -= 2;
            currentPlayerImg = imgBack;
            if (playerY == 100 && !dialogueTriggered[2]) {
                setDialogue("Baiklah. Mari kita selesaikan shift malam ini.");
                dialogueTriggered[2] = true;
            }
            if (playerY <= -50) { 
                currentPlayerImg = null; 
                animPhase = 5;
                animationTimer.stop();
                btnOpenDoor.setVisible(true); 
                btnSkip.setVisible(false); // Sembunyikan tombol skip saat tombol pintu muncul
            }
        }
    }
    
    private Image loadSafeImage(String path) {
        URL url = getClass().getResource(path);
        if (url != null) return new ImageIcon(url).getImage();
        return null;
    }
    
    private void setDialogue(String text) {
        fullText = text;
        textArea.setText("");
        charIndex = 0;
        isTextFinished = false;
        typewriterTimer.start();
    }

    private void processTypewriterEffect() {
        if (charIndex < fullText.length()) {
            textArea.append(String.valueOf(fullText.charAt(charIndex)));
            charIndex++;
        } else {
            typewriterTimer.stop();
            isTextFinished = true;
        }
    }

    private void setupInteraction() {
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isTextFinished) {
                    typewriterTimer.stop();
                    textArea.setText(fullText);
                    isTextFinished = true;
                }
            }
        };
        addMouseListener(clickListener);
        textArea.addMouseListener(clickListener);
    }
}
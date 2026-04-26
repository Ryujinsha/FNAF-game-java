package com.ryujinsha.engine;

import com.ryujinsha.system.AssetCache;
import javax.swing.*;
import java.awt.*;

/**
 * ✨ MainMenuGUI — Menu Utama Game dengan Halaman Pengaturan & Pencapaian
 */
public class MainMenuGUI extends JPanel {
    private MainFrame mainFrame;
    private CardLayout cardLayout;
    private JPanel mainContainer;

    public MainMenuGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.cardLayout = new CardLayout();
        this.mainContainer = new JPanel(cardLayout);

        setLayout(new BorderLayout());
        add(mainContainer, BorderLayout.CENTER);

        // Tambahkan halaman-halaman menu
        mainContainer.add(createMainMenuPanel(), "MAIN");
        mainContainer.add(createSettingsPanel(), "SETTINGS");
        mainContainer.add(createAchievementsPanel(), "ACHIEVEMENTS");

        cardLayout.show(mainContainer, "MAIN");
    }

    // ============================================================
    // 1. HALAMAN MENU UTAMA
    // ============================================================
    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel() {
            private Image enemyImage;
            {
                // ✨ FIX: Menggunakan AssetCache agar lebih ringan
                enemyImage = AssetCache.get("/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png");
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());

                if (enemyImage != null) {
                    int imgWidth = 450;
                    int imgHeight = 650;
                    int x = (getWidth() - imgWidth) / 2;
                    int y = (getHeight() - imgHeight) / 2 - 50;
                    g.drawImage(enemyImage, x, y, imgWidth, imgHeight, this);
                }
                
                // Overlay gelap sedikit agar teks terbaca
                g.setColor(new Color(0, 0, 0, 80));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new GridBagLayout());

        JPanel uiPanel = new JPanel();
        uiPanel.setOpaque(false);
        uiPanel.setLayout(new BoxLayout(uiPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("NIGHT SHIFT SURVIVAL");
        titleLabel.setFont(new Font("Consolas", Font.BOLD, 75));
        titleLabel.setForeground(Color.RED);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnPlay = createStyledButton("START SHIFT", Color.GREEN);
        JButton btnSettings = createStyledButton("SETTINGS", Color.WHITE);
        JButton btnAchievements = createStyledButton("ACHIEVEMENTS", Color.YELLOW);
        JButton btnQuit = createStyledButton("QUIT", Color.LIGHT_GRAY);

        btnPlay.addActionListener(e -> mainFrame.showScreen("CUTSCENE"));
        btnSettings.addActionListener(e -> cardLayout.show(mainContainer, "SETTINGS"));
        btnAchievements.addActionListener(e -> cardLayout.show(mainContainer, "ACHIEVEMENTS"));
        btnQuit.addActionListener(e -> System.exit(0));

        uiPanel.add(Box.createVerticalStrut(350));
        uiPanel.add(titleLabel);
        uiPanel.add(Box.createVerticalStrut(60));
        uiPanel.add(btnPlay);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnSettings);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnAchievements);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnQuit);

        panel.add(uiPanel);
        return panel;
    }

    // ============================================================
    // 2. HALAMAN PENGATURAN (SETTINGS)
    // ============================================================
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK);
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JLabel title = new JLabel("SETTINGS", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 60));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        content.add(Box.createVerticalStrut(100));
        content.add(createSettingRow("Master Volume", 80));
        content.add(Box.createVerticalStrut(30));
        content.add(createSettingRow("Graphics Quality", "HIGH"));
        content.add(Box.createVerticalStrut(30));
        content.add(createSettingRow("Fullscreen Mode", "ENABLED"));

        panel.add(content, BorderLayout.CENTER);

        JButton btnBack = createStyledButton("BACK TO MENU", Color.RED);
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "MAIN"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    // 3. HALAMAN PENCAPAIAN (ACHIEVEMENTS)
    // ============================================================
    private JPanel createAchievementsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(15, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 100, 50, 100));

        JLabel title = new JLabel("ACHIEVEMENTS", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 60));
        title.setForeground(Color.YELLOW);
        panel.add(title, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        listPanel.setOpaque(false);
        
        listPanel.add(createAchievementItem("First Night", "Selesaikan malam pertama kamu.", true));
        listPanel.add(createAchievementItem("Ninja Guard", "Bersembunyi 10 kali tanpa tertangkap.", false));
        listPanel.add(createAchievementItem("Master Thief", "Berhasil mencongkel pintu tercepat.", false));
        listPanel.add(createAchievementItem("Survivor", "Selesaikan game dalam tingkat kesulitan HARD.", false));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnBack = createStyledButton("BACK TO MENU", Color.RED);
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "MAIN"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 28));
        btn.setBackground(new Color(40, 40, 40));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(500, 50));
        return btn;
    }

    private JPanel createSettingRow(String label, Object value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(800, 40));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Consolas", Font.PLAIN, 24));
        lbl.setForeground(Color.GRAY);
        
        JLabel val = new JLabel(value.toString());
        val.setFont(new Font("Consolas", Font.BOLD, 24));
        val.setForeground(Color.WHITE);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel createAchievementItem(String name, String desc, boolean unlocked) {
        JPanel item = new JPanel(new BorderLayout());
        item.setBackground(new Color(30, 30, 30));
        item.setBorder(BorderFactory.createLineBorder(unlocked ? Color.YELLOW : Color.DARK_GRAY, 2));
        
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel nameLbl = new JLabel(name + (unlocked ? " [COMPLETED]" : " [LOCKED]"));
        nameLbl.setFont(new Font("Consolas", Font.BOLD, 22));
        nameLbl.setForeground(unlocked ? Color.YELLOW : Color.GRAY);
        
        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Consolas", Font.PLAIN, 16));
        descLbl.setForeground(Color.LIGHT_GRAY);
        
        text.add(nameLbl);
        text.add(descLbl);
        item.add(text, BorderLayout.CENTER);
        
        return item;
    }
}
package com.ryujinsha.engine;

import com.ryujinsha.system.AssetCache;
import com.ryujinsha.system.AudioManager;
import com.ryujinsha.system.ResourceManaged;
import javax.swing.*;
import java.awt.*;

/**
 * ✨ MainMenuGUI — Menu Utama Game dengan Halaman Pengaturan & Pencapaian
 */
public class MainMenuGUI extends JPanel implements ResourceManaged {
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
        mainContainer.add(createAboutPanel(), "ABOUT");

        cardLayout.show(mainContainer, "MAIN");

        // ✨ Play Main Menu BGM
        AudioManager.playBGM("/assets/audio/bgm/Basement Bellows.wav");
    }

    // ============================================================
    // 1. HALAMAN MENU UTAMA
    // ============================================================
    private JPanel createMainMenuPanel() {
        JPanel panel = new JPanel() {
            private Image bgImage;
            {
                bgImage = AssetCache.get("/assets/backgrounds/main_menu_bg.png");
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (bgImage != null) {
                    g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                
                // Vignette/Overlay gradient for better atmosphere
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 0, 0, 100), 0, getHeight(), new Color(0, 0, 0, 200));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new GridBagLayout());

        JPanel uiPanel = new JPanel();
        uiPanel.setOpaque(false);
        uiPanel.setLayout(new BoxLayout(uiPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("THE LAST DOOR");
        titleLabel.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 110));
        titleLabel.setForeground(new Color(150, 0, 0));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add shadow effect to title using HTML
        titleLabel.setText("<html><body style='text-shadow: 5px 5px 10px #000000;'>THE LAST DOOR</body></html>");

        JButton btnStory = createStyledButton("STORY", Color.WHITE);
        JButton btnEndless = createStyledButton("ENDLESS", Color.LIGHT_GRAY);
        JButton btnSetting = createStyledButton("SETTING", Color.WHITE);
        JButton btnAbout = createStyledButton("ABOUT", Color.WHITE);
        JButton btnQuit = createStyledButton("QUIT", Color.RED);

        btnStory.addActionListener(e -> mainFrame.showScreen("CUTSCENE"));
        btnEndless.addActionListener(e -> JOptionPane.showMessageDialog(this, "Endless Mode - Coming Soon!", "Info", JOptionPane.INFORMATION_MESSAGE));
        btnSetting.addActionListener(e -> cardLayout.show(mainContainer, "SETTINGS"));
        btnAbout.addActionListener(e -> cardLayout.show(mainContainer, "ABOUT"));
        btnQuit.addActionListener(e -> System.exit(0));

        uiPanel.add(Box.createVerticalStrut(250));
        uiPanel.add(titleLabel);
        uiPanel.add(Box.createVerticalStrut(80));
        uiPanel.add(btnStory);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnEndless);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnSetting);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnAbout);
        uiPanel.add(Box.createVerticalStrut(15));
        uiPanel.add(btnQuit);

        panel.add(uiPanel);

        // --- Pojok Kanan Atas: Profile & Leaderboard ---
        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        topRightPanel.setOpaque(false);
        
        JButton btnProfile = createSmallStyledButton("PROFILE");
        JButton btnLeaderboard = createSmallStyledButton("LEADERBOARD");
        
        btnProfile.addActionListener(e -> JOptionPane.showMessageDialog(this, "Profile - Coming Soon!", "Info", JOptionPane.INFORMATION_MESSAGE));
        btnLeaderboard.addActionListener(e -> JOptionPane.showMessageDialog(this, "Leaderboard - Coming Soon!", "Info", JOptionPane.INFORMATION_MESSAGE));
        
        topRightPanel.add(btnProfile);
        topRightPanel.add(btnLeaderboard);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_END;
        panel.add(topRightPanel, gbc);

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
        content.add(Box.createVerticalStrut(30));
        content.add(createDevModeRow());

        panel.add(content, BorderLayout.CENTER);

        JButton btnBack = createStyledButton("BACK TO MENU", Color.RED);
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "MAIN"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    // ============================================================
    // 3. HALAMAN ABOUT
    // ============================================================
    private JPanel createAboutPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(10, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        JLabel title = new JLabel("CREDITS", SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 50));
        title.setForeground(Color.WHITE);
        panel.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Developer Section
        JLabel devTitle = new JLabel("DEVELOPMENT TEAM");
        devTitle.setFont(new Font("Consolas", Font.BOLD, 24));
        devTitle.setForeground(Color.LIGHT_GRAY);
        devTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(devTitle);
        contentPanel.add(Box.createVerticalStrut(20));

        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 20, 20));
        gridPanel.setOpaque(false);
        gridPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String devImg = "/assets/dev/dev.jpg";
        gridPanel.add(createDevCard("Krisna Dean Noven", "Project Manager", devImg));
        gridPanel.add(createDevCard("Muhammad Faried", "Game Developer", devImg));
        gridPanel.add(createDevCard("Galang Sukmagama", "Game Designer", devImg));
        gridPanel.add(createDevCard("Raihan Nazriel", "QA Tester", devImg));
        gridPanel.add(createDevCard("Nadia Zahra A.", "Game Designer", devImg));
        
        contentPanel.add(gridPanel);

        // Campus Tribute Section
        contentPanel.add(Box.createVerticalStrut(40));
        JLabel campusTitle = new JLabel("TRIBUTE TO CAMPUS");
        campusTitle.setFont(new Font("Consolas", Font.BOLD, 24));
        campusTitle.setForeground(Color.LIGHT_GRAY);
        campusTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(campusTitle);
        contentPanel.add(Box.createVerticalStrut(20));

        Image campusImg = com.ryujinsha.system.AssetCache.get("/assets/dev/horizon.png");
        if (campusImg != null) {
            // Skala logo kampus menjadi ukuran yang pantas
            int targetWidth = 300;
            int targetHeight = (campusImg.getHeight(null) * targetWidth) / Math.max(1, campusImg.getWidth(null));
            JLabel campusLabel = new JLabel(new ImageIcon(campusImg.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)));
            campusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            contentPanel.add(campusLabel);
        }

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        // Scrollbar tidak terlihat tapi bisa di scroll
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0,0));
        
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnBack = createStyledButton("BACK TO MENU", Color.RED);
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, "MAIN"));
        panel.add(btnBack, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDevCard(String name, String role, String imgPath) {
        JPanel card = new JPanel(new BorderLayout(15, 10));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 50), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        Image img = com.ryujinsha.system.AssetCache.get(imgPath);
        if (img != null) {
            // Resize menjadi bulat/kotak kecil
            Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            card.add(new JLabel(new ImageIcon(scaled)), BorderLayout.WEST);
        }

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);
        
        JLabel roleLabel = new JLabel(role);
        roleLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        roleLabel.setForeground(Color.RED);

        textPanel.add(nameLabel);
        textPanel.add(roleLabel);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 32));
        btn.setForeground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(600, 60));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover Effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(new Color(255, 50, 50));
                btn.setText("> " + text + " <");
                AudioManager.playSound("/assets/audio/sfx/button_click.wav"); // Subtle sound on hover if available
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(color);
                btn.setText(text);
            }
        });

        return btn;
    }

    private JButton createSmallStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 18));
        btn.setForeground(Color.GRAY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.WHITE);
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.GRAY);
            }
        });
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

    private JPanel createDevModeRow() {
        JPanel row = createSettingRow("Developer Mode", MainFrame.isDevMode ? "ENABLED" : "DISABLED");
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!MainFrame.isDevMode) {
                    String pass = JOptionPane.showInputDialog(MainMenuGUI.this, "ENTER DEVELOPER PASSWORD:");
                    if ("ambatukam".equals(pass)) {
                        MainFrame.isDevMode = true;
                        JOptionPane.showMessageDialog(MainMenuGUI.this, "DEVELOPER MODE ACTIVATED!");
                        cardLayout.show(mainContainer, "SETTINGS"); // Refresh
                    } else if (pass != null) {
                        JOptionPane.showMessageDialog(MainMenuGUI.this, "WRONG PASSWORD!");
                    }
                } else {
                    MainFrame.isDevMode = false;
                    cardLayout.show(mainContainer, "SETTINGS"); // Refresh
                }
            }
        });
        return row;
    }

    @Override
    public void stopAllProcesses() {
        System.out.println("[MENU] Stopping Menu Processes...");
        // BGM dihentikan oleh MainFrame atau screen berikutnya jika perlu, 
        // tapi kita panggil stopAllSounds di sini agar aman.
        AudioManager.stopAllSounds();
    }
}
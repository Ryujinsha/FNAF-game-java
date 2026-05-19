package com.ryujinsha.engine;

import com.ryujinsha.database.LeaderboardDAO;
import com.ryujinsha.database.LeaderboardEntry;
import com.ryujinsha.system.AudioManager;
import com.ryujinsha.system.ResourceManaged;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * ✨ LeaderboardGUI — Halaman Leaderboard dengan dua tab: STORY dan ENDLESS.
 *
 * Desain horror/gelap konsisten dengan tema game "The Last Door".
 * Story Mode: ranking berdasarkan waktu tercepat.
 * Endless Mode: ranking berdasarkan poin tertinggi.
 */
public class LeaderboardGUI extends JPanel implements ResourceManaged {

    private MainFrame mainFrame;
    private String activeTab = "STORY"; // Tab aktif saat ini
    private JPanel contentPanel;        // Panel konten yang di-refresh saat ganti tab

    // Warna tema
    private static final Color BG_COLOR = new Color(10, 10, 15);
    private static final Color CARD_BG = new Color(20, 20, 28);
    private static final Color HEADER_COLOR = new Color(150, 0, 0);
    private static final Color TEXT_PRIMARY = new Color(220, 220, 220);
    private static final Color TEXT_SECONDARY = new Color(120, 120, 120);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color SILVER = new Color(192, 192, 192);
    private static final Color BRONZE = new Color(205, 127, 50);
    private static final Color VICTORY_COLOR = new Color(0, 200, 0);
    private static final Color DEFEAT_COLOR = new Color(200, 0, 0);
    private static final Color TAB_ACTIVE = new Color(150, 0, 0);
    private static final Color TAB_INACTIVE = new Color(40, 40, 50);

    private static final int MAX_ENTRIES = 15;

    public LeaderboardGUI(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(BG_COLOR);
        setLayout(new BorderLayout());

        // ============================================================
        // HEADER
        // ============================================================
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 10, 50));

        JLabel titleLabel = new JLabel("LEADERBOARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD | Font.ITALIC, 64));
        titleLabel.setForeground(HEADER_COLOR);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // ============================================================
        // TAB BAR + CONTENT
        // ============================================================
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_COLOR);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(0, 80, 0, 80));

        // Tab bar
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tabBar.setBackground(BG_COLOR);

        JButton tabStory = createTabButton("⏱ STORY MODE", "STORY");
        JButton tabEndless = createTabButton("🏆 ENDLESS MODE", "ENDLESS");

        tabBar.add(tabStory);
        tabBar.add(tabEndless);
        centerWrapper.add(tabBar, BorderLayout.NORTH);

        // Content area
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_COLOR);
        centerWrapper.add(contentPanel, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        // ============================================================
        // FOOTER — Tombol kembali
        // ============================================================
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 30, 0));

        JButton btnBack = createStyledButton("BACK TO MENU");
        btnBack.addActionListener(e -> mainFrame.showScreen("MENU"));
        footerPanel.add(btnBack);

        add(footerPanel, BorderLayout.SOUTH);

        // Load data awal
        refreshContent();
    }

    // ============================================================
    // TAB BUTTON FACTORY
    // ============================================================
    private JButton createTabButton(String text, String tabName) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 22));
        btn.setForeground(Color.WHITE);
        btn.setBackground(tabName.equals(activeTab) ? TAB_ACTIVE : TAB_INACTIVE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(300, 50));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            activeTab = tabName;
            refreshContent();
            // Update semua tab button colors
            Container parent = btn.getParent();
            if (parent != null) {
                for (Component c : parent.getComponents()) {
                    if (c instanceof JButton) {
                        ((JButton) c).setBackground(TAB_INACTIVE);
                    }
                }
            }
            btn.setBackground(TAB_ACTIVE);
        });

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!tabName.equals(activeTab)) {
                    btn.setBackground(new Color(80, 0, 0));
                }
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(tabName.equals(activeTab) ? TAB_ACTIVE : TAB_INACTIVE);
            }
        });

        return btn;
    }

    // ============================================================
    // REFRESH CONTENT
    // ============================================================
    private void refreshContent() {
        contentPanel.removeAll();

        if ("STORY".equals(activeTab)) {
            contentPanel.add(buildStoryLeaderboard(), BorderLayout.CENTER);
        } else {
            contentPanel.add(buildEndlessLeaderboard(), BorderLayout.CENTER);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ============================================================
    // BUILD STORY LEADERBOARD
    // ============================================================
    private JScrollPane buildStoryLeaderboard() {
        List<LeaderboardEntry> entries = LeaderboardDAO.getTopStoryScores(MAX_ENTRIES);
        return buildTable(entries, true);
    }

    // ============================================================
    // BUILD ENDLESS LEADERBOARD
    // ============================================================
    private JScrollPane buildEndlessLeaderboard() {
        List<LeaderboardEntry> entries = LeaderboardDAO.getTopEndlessScores(MAX_ENTRIES);
        return buildTable(entries, false);
    }

    // ============================================================
    // BUILD TABLE (Custom Painted)
    // ============================================================
    private JScrollPane buildTable(List<LeaderboardEntry> entries, boolean isStoryMode) {
        JPanel tablePanel = new JPanel();
        tablePanel.setBackground(BG_COLOR);
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // Header Row
        JPanel headerRow = createRow(-1, "RANK", "NAMA", isStoryMode ? "WAKTU" : "POIN", "STATUS", "TANGGAL", true, null);
        tablePanel.add(headerRow);
        tablePanel.add(Box.createVerticalStrut(5));

        // Separator
        JPanel sep = new JPanel();
        sep.setBackground(new Color(50, 50, 60));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        sep.setPreferredSize(new Dimension(0, 2));
        tablePanel.add(sep);
        tablePanel.add(Box.createVerticalStrut(5));

        if (entries.isEmpty()) {
            JLabel emptyLabel = new JLabel("Belum ada data. Main dulu!", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Consolas", Font.ITALIC, 20));
            emptyLabel.setForeground(TEXT_SECONDARY);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tablePanel.add(Box.createVerticalStrut(50));
            tablePanel.add(emptyLabel);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                LeaderboardEntry entry = entries.get(i);
                String rank = getRankLabel(i + 1);
                String name = entry.getPlayerName();
                String value = isStoryMode ? entry.getFormattedTime() : String.valueOf(entry.getScore());
                String status = entry.isVictory() ? "🏆 VICTORY" : "💀 DEFEAT";
                String date = entry.getPlayedAt() != null ? entry.getPlayedAt().substring(0, 10) : "-";
                Color rankColor = getRankColor(i + 1);

                JPanel row = createRow(i + 1, rank, name, value, status, date, false, entry);
                tablePanel.add(row);
                tablePanel.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scrollPane = new JScrollPane(tablePanel);
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    // ============================================================
    // CREATE TABLE ROW
    // ============================================================
    private JPanel createRow(int rank, String rankText, String name, String value, String status, String date, boolean isHeader, LeaderboardEntry entry) {
        JPanel row = new JPanel(new GridLayout(1, 5, 10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.setPreferredSize(new Dimension(0, 50));
        row.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        if (isHeader) {
            row.setBackground(new Color(30, 30, 40));
        } else if (rank <= 3) {
            row.setBackground(new Color(25, 25, 35));
        } else {
            row.setBackground(rank % 2 == 0 ? CARD_BG : new Color(15, 15, 22));
        }

        Font font = isHeader
                ? new Font("Consolas", Font.BOLD, 16)
                : new Font("Consolas", Font.PLAIN, 16);

        Color textColor = isHeader ? GOLD : TEXT_PRIMARY;
        Color rankColor = isHeader ? GOLD : getRankColor(rank);
        Color statusColor = isHeader ? GOLD : (entry != null && entry.isVictory() ? VICTORY_COLOR : DEFEAT_COLOR);

        row.add(createCellLabel(rankText, font, rankColor, SwingConstants.CENTER));
        row.add(createCellLabel(name, font, textColor, SwingConstants.LEFT));
        row.add(createCellLabel(value, font, textColor, SwingConstants.CENTER));
        row.add(createCellLabel(status, font, statusColor, SwingConstants.CENTER));
        row.add(createCellLabel(date, font, TEXT_SECONDARY, SwingConstants.CENTER));

        return row;
    }

    private JLabel createCellLabel(String text, Font font, Color color, int alignment) {
        JLabel label = new JLabel(text, alignment);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    // ============================================================
    // RANK HELPERS
    // ============================================================
    private String getRankLabel(int rank) {
        return switch (rank) {
            case 1 -> "🥇 1st";
            case 2 -> "🥈 2nd";
            case 3 -> "🥉 3rd";
            default -> "#" + rank;
        };
    }

    private Color getRankColor(int rank) {
        return switch (rank) {
            case 1 -> GOLD;
            case 2 -> SILVER;
            case 3 -> BRONZE;
            default -> TEXT_PRIMARY;
        };
    }

    // ============================================================
    // STYLED BUTTON
    // ============================================================
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 28));
        btn.setForeground(Color.RED);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(new Color(255, 50, 50));
                btn.setText("> " + text + " <");
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.RED);
                btn.setText(text);
            }
        });

        return btn;
    }

    @Override
    public void stopAllProcesses() {
        System.out.println("[LEADERBOARD] Stopping Leaderboard processes...");
    }
}

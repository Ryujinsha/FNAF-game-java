package com.ryujinsha.engine;

import com.ryujinsha.database.DatabaseManager;
import com.ryujinsha.database.LeaderboardDAO;

import javax.swing.*;
import java.awt.*;

/**
 * ✨ ScoreSaver — Utility untuk menampilkan dialog input nama
 * dan menyimpan skor ke database.
 *
 * Dipanggil setiap kali game berakhir (VICTORY atau DEFEAT).
 */
public class ScoreSaver {

    /**
     * Tampilkan dialog input nama pemain dan simpan skor ke database.
     *
     * @param ctx    GameContext yang berisi data game (timer, mode, dll.)
     * @param status "VICTORY" atau "DEFEAT"
     */
    public static void showNameInputAndSave(GameContext ctx, String status) {
        if (!DatabaseManager.isInitialized()) {
            System.err.println("⚠️ [SCORE] Database belum terinisialisasi. Skor tidak disimpan.");
            return;
        }

        // Hitung waktu bermain
        long elapsedMs = ctx.gameEndTimeMs - ctx.gameStartTimeMs;
        if (elapsedMs < 0) elapsedMs = 0;

        String timeFormatted = formatTime(elapsedMs);
        String statusEmoji = status.equals("VICTORY") ? "🏆" : "💀";

        // Buat panel custom untuk dialog
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.BLACK);

        JLabel headerLabel = new JLabel(statusEmoji + " " + status + " " + statusEmoji);
        headerLabel.setFont(new Font("Consolas", Font.BOLD, 28));
        headerLabel.setForeground(status.equals("VICTORY") ? new Color(0, 220, 0) : new Color(220, 0, 0));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timeLabel = new JLabel("Waktu: " + timeFormatted);
        timeLabel.setFont(new Font("Consolas", Font.PLAIN, 18));
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel promptLabel = new JLabel("Masukkan nama untuk leaderboard:");
        promptLabel.setFont(new Font("Consolas", Font.PLAIN, 16));
        promptLabel.setForeground(Color.LIGHT_GRAY);
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Consolas", Font.BOLD, 20));
        nameField.setMaximumSize(new Dimension(300, 40));
        nameField.setHorizontalAlignment(JTextField.CENTER);

        panel.add(Box.createVerticalStrut(10));
        panel.add(headerLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(timeLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(promptLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(nameField);

        int result = JOptionPane.showConfirmDialog(
                null, panel, "SAVE SCORE", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                playerName = "Anonymous";
            }
            // Batasi panjang nama
            if (playerName.length() > 50) {
                playerName = playerName.substring(0, 50);
            }

            // Simpan berdasarkan mode
            if ("STORY".equals(ctx.currentGameMode)) {
                LeaderboardDAO.insertStoryScore(playerName, elapsedMs, status);
            } else if ("ENDLESS".equals(ctx.currentGameMode)) {
                LeaderboardDAO.insertEndlessScore(playerName, ctx.endlessScore, status);
            }

            ctx.logEvent("💾 [SAVED] Skor disimpan: " + playerName + " | " + timeFormatted + " | " + status);
        } else {
            ctx.logEvent("⏭️ [SKIP] Pemain melewatkan penyimpanan skor.");
        }
    }

    /**
     * Format milidetik ke string MM:SS.mmm
     */
    private static String formatTime(long totalMs) {
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }
}

package com.ryujinsha.database;

/**
 * ✨ LeaderboardEntry — Data model untuk satu entry leaderboard.
 *
 * Digunakan untuk menampung data dari database dan menampilkannya di GUI.
 * Mendukung dua mode: Story (time-based) dan Endless (score-based).
 */
public class LeaderboardEntry {

    private int id;
    private String playerName;
    private long completionTimeMs;  // Untuk Story Mode
    private int score;               // Untuk Endless Mode
    private String status;           // "VICTORY" atau "DEFEAT"
    private String playedAt;         // Timestamp kapan dimainkan

    // ============================================================
    // CONSTRUCTOR UNTUK STORY MODE
    // ============================================================
    public LeaderboardEntry(int id, String playerName, long completionTimeMs, String status, String playedAt) {
        this.id = id;
        this.playerName = playerName;
        this.completionTimeMs = completionTimeMs;
        this.status = status;
        this.playedAt = playedAt;
    }

    // ============================================================
    // CONSTRUCTOR UNTUK ENDLESS MODE
    // ============================================================
    public LeaderboardEntry(int id, String playerName, int score, String status, String playedAt) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.status = status;
        this.playedAt = playedAt;
    }

    // ============================================================
    // GETTERS
    // ============================================================
    public int getId() { return id; }
    public String getPlayerName() { return playerName; }
    public long getCompletionTimeMs() { return completionTimeMs; }
    public int getScore() { return score; }
    public String getStatus() { return status; }
    public String getPlayedAt() { return playedAt; }

    // ============================================================
    // FORMATTED TIME
    // ============================================================

    /**
     * Format waktu milidetik ke format MM:SS.mmm
     * Contoh: 135342 ms → "02:15.342"
     */
    public String getFormattedTime() {
        long totalMs = completionTimeMs;
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        long millis = totalMs % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    /**
     * Format waktu singkat tanpa milidetik: MM:SS
     */
    public String getFormattedTimeShort() {
        long totalMs = completionTimeMs;
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Cek apakah entry ini adalah victory.
     */
    public boolean isVictory() {
        return "VICTORY".equals(status);
    }
}

package com.ryujinsha.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✨ LeaderboardDAO — Data Access Object untuk operasi CRUD leaderboard.
 *
 * Menyediakan method untuk menyimpan dan mengambil skor dari
 * tabel story_leaderboard dan endless_leaderboard.
 */
public class LeaderboardDAO {

    // ============================================================
    // STORY MODE — Insert
    // ============================================================

    /**
     * Simpan skor Story Mode ke database.
     *
     * @param playerName Nama pemain
     * @param timeMs     Waktu bermain dalam milidetik
     * @param status     "VICTORY" atau "DEFEAT"
     */
    public static void insertStoryScore(String playerName, long timeMs, String status) {
        String sql = "INSERT INTO story_leaderboard (player_name, completion_time_ms, status) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            pstmt.setLong(2, timeMs);
            pstmt.setString(3, status);
            pstmt.executeUpdate();

            System.out.println("✅ [DB] Story score saved: " + playerName + " | " + timeMs + "ms | " + status);

        } catch (SQLException e) {
            System.err.println("❌ [DB] Failed to insert story score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // STORY MODE — Query
    // ============================================================

    /**
     * Ambil top N skor Story Mode.
     * VICTORY diurutkan berdasarkan waktu tercepat (ASC).
     * DEFEAT ditampilkan setelah semua VICTORY, diurutkan berdasarkan waktu terlama bermain (DESC).
     */
    public static List<LeaderboardEntry> getTopStoryScores(int limit) {
        String sql = """
            (SELECT * FROM story_leaderboard WHERE status = 'VICTORY' ORDER BY completion_time_ms ASC)
            UNION ALL
            (SELECT * FROM story_leaderboard WHERE status = 'DEFEAT' ORDER BY completion_time_ms DESC)
            LIMIT ?
        """;

        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                    rs.getInt("id"),
                    rs.getString("player_name"),
                    rs.getLong("completion_time_ms"),
                    rs.getString("status"),
                    rs.getString("played_at")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ [DB] Failed to fetch story scores: " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }

    // ============================================================
    // ENDLESS MODE — Insert
    // ============================================================

    /**
     * Simpan skor Endless Mode ke database.
     *
     * @param playerName Nama pemain
     * @param score      Poin yang didapat
     * @param status     "VICTORY" atau "DEFEAT"
     */
    public static void insertEndlessScore(String playerName, int score, String status) {
        String sql = "INSERT INTO endless_leaderboard (player_name, score, status) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            pstmt.setInt(2, score);
            pstmt.setString(3, status);
            pstmt.executeUpdate();

            System.out.println("✅ [DB] Endless score saved: " + playerName + " | " + score + " pts | " + status);

        } catch (SQLException e) {
            System.err.println("❌ [DB] Failed to insert endless score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // ENDLESS MODE — Query
    // ============================================================

    /**
     * Ambil top N skor Endless Mode, diurutkan berdasarkan poin tertinggi (DESC).
     */
    public static List<LeaderboardEntry> getTopEndlessScores(int limit) {
        String sql = "SELECT * FROM endless_leaderboard ORDER BY score DESC LIMIT ?";

        List<LeaderboardEntry> entries = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                    rs.getInt("id"),
                    rs.getString("player_name"),
                    rs.getInt("score"),
                    rs.getString("status"),
                    rs.getString("played_at")
                ));
            }

        } catch (SQLException e) {
            System.err.println("❌ [DB] Failed to fetch endless scores: " + e.getMessage());
            e.printStackTrace();
        }

        return entries;
    }
}

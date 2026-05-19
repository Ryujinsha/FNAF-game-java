package com.ryujinsha.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ✨ DatabaseManager — Singleton untuk koneksi MySQL.
 *
 * Auto-create database dan tabel jika belum ada.
 * Konfigurasi default: localhost:3306, user=root, password="".
 */
public class DatabaseManager {

    // ============================================================
    // KONFIGURASI KONEKSI
    // ============================================================
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "thelastdoor_db";
    private static final String USER = "root";
    private static final String PASSWORD = "admin123";

    private static final String BASE_URL = "jdbc:mysql://" + HOST + ":" + PORT;
    private static final String DB_URL = BASE_URL + "/" + DATABASE + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static boolean initialized = false;

    // ============================================================
    // INISIALISASI
    // ============================================================

    /**
     * Inisialisasi database: buat database dan tabel jika belum ada.
     * Dipanggil sekali saat aplikasi dimulai.
     */
    public static void initialize() {
        if (initialized) return;

        try {
            // Load driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Buat database jika belum ada
            try (Connection conn = DriverManager.getConnection(BASE_URL + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", USER, PASSWORD);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATABASE);
                System.out.println("✅ [DB] Database '" + DATABASE + "' ready.");
            }

            // Buat tabel-tabel
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                // Tabel Story Leaderboard (ranking by fastest time)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS story_leaderboard (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        player_name VARCHAR(50) NOT NULL,
                        completion_time_ms BIGINT NOT NULL,
                        status ENUM('VICTORY', 'DEFEAT') NOT NULL,
                        played_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                // Tabel Endless Leaderboard (ranking by highest score)
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS endless_leaderboard (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        player_name VARCHAR(50) NOT NULL,
                        score INT NOT NULL,
                        status ENUM('VICTORY', 'DEFEAT') NOT NULL,
                        played_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                """);

                System.out.println("✅ [DB] Tables 'story_leaderboard' & 'endless_leaderboard' ready.");
            }

            initialized = true;

        } catch (ClassNotFoundException e) {
            System.err.println("❌ [DB] MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ [DB] Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // KONEKSI
    // ============================================================

    /**
     * Mendapatkan koneksi ke database MySQL.
     * Pastikan initialize() sudah dipanggil sebelumnya.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /**
     * Cek apakah database sudah terinisialisasi dengan baik.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Shutdown — tutup resource jika diperlukan.
     */
    public static void shutdown() {
        initialized = false;
        System.out.println("🔌 [DB] Database manager shut down.");
    }
}

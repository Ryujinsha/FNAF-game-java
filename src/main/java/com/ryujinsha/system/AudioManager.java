package com.ryujinsha.system;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class AudioManager {
    
    // Memutar SFX (Sekali putar)
    public static void playSound(String path) {
        try {
            // Membaca dari resource Maven (direktori assets)
            InputStream audioSrc = AudioManager.class.getResourceAsStream(path);
            if (audioSrc == null) {
                System.err.println("[AUDIO ERROR] File tidak ditemukan: " + path);
                return;
            }
            // Menggunakan BufferedInputStream agar kompatibel dengan AudioSystem
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("[AUDIO ERROR] Gagal memutar " + path + ": " + e.getMessage());
        }
    }

    // Menghentikan BGM atau suara panjang (jika Anda menambahkannya nanti)
    public static void stopAllSounds() {
        System.out.println("[AUDIO] Menghentikan semua background music...");
        // Implementasi logika stop() untuk Clip BGM yang sedang berjalan diletakkan di sini nanti.
    }
}
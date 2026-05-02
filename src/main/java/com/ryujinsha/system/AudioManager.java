package com.ryujinsha.system;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class AudioManager {
    private static Clip bgmClip;

    // Memutar SFX (Sekali putar)
    public static void playSound(String path) {
        try {
            InputStream audioSrc = AudioManager.class.getResourceAsStream(path);
            if (audioSrc == null) {
                System.err.println("[AUDIO ERROR] File tidak ditemukan: " + path);
                return;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("[AUDIO ERROR] Gagal memutar " + path + ": " + e.getMessage());
        }
    }

    // Memutar BGM dengan opsi looping
    public static void playBGM(String path) {
        try {
            stopAllSounds(); // Hentikan BGM sebelumnya jika ada
            
            InputStream audioSrc = AudioManager.class.getResourceAsStream(path);
            if (audioSrc == null) {
                System.err.println("[BGM ERROR] File tidak ditemukan: " + path);
                return;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioStream);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop terus menerus
            bgmClip.start();
            System.out.println("[AUDIO] Memutar BGM: " + path);
        } catch (Exception e) {
            System.err.println("[AUDIO ERROR] Gagal memutar BGM " + path + ": " + e.getMessage());
        }
    }

    // Menghentikan BGM atau suara panjang
    public static void stopAllSounds() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
            System.out.println("[AUDIO] Background music dihentikan.");
        }
    }
}
package com.ryujinsha.system;

public class SanitySystem {
    private int sanityLevel;

    public SanitySystem() {
        this.sanityLevel = 100; // Mulai dengan mental sehat 100%
    }

    public int getSanityLevel() {
        return sanityLevel;
    }

    // Dipanggil setiap tick jika Player sedang buka Tablet atau Tutup Pintu
    public void dropSanity(int amount) {
        sanityLevel -= amount;
        if (sanityLevel < 0) {
            sanityLevel = 0;
        }
        System.out.println("[SYSTEM] Sanity dropping... Current: " + sanityLevel + "%");
    }

    // (Opsional) Jika kamu mau ada mekanik memulihkan sanity saat diam/aman
    public void recoverSanity(int amount) {
        sanityLevel += amount;
        if (sanityLevel > 100) {
            sanityLevel = 100;
        }
    }

    public boolean isInsane() {
        return sanityLevel <= 0;
    }
}
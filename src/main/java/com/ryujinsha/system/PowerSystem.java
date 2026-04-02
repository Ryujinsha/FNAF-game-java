package com.ryujinsha.system;

public class PowerSystem {
    private int currentPower;

    public PowerSystem() {
        this.currentPower = 100; // Start dengan 100%
    }

    public int getCurrentPower() {
        return currentPower;
    }

    // Method untuk mengurangi power, dengan proteksi agar tidak minus
    public void consumePower(int amount) {
        currentPower -= amount;
        if (currentPower < 0) {
            currentPower = 0;
        }
    }

    public void decreasePower(int amount) {
        // Asumsi variabel penyimpan listrik Anda bernama 'currentPower'
        // Jika namanya berbeda (misal 'powerLevel'), silakan sesuaikan.
        this.currentPower -= amount;
        
        // Memastikan listrik tidak tembus minus di bawah 0
        if (this.currentPower < 0) {
            this.currentPower = 0;
        }
    }

    // ✨ METHOD BARU: Untuk memberikan listrik cadangan (Second Chance)
    public void addPower(int amount) {
        this.currentPower += amount;
        if (this.currentPower > 100) {
            this.currentPower = 100;
        }
    }

    public boolean isPowerEmpty() {
        return currentPower <= 0;
    }
}

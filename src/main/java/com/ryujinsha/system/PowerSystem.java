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

    public boolean isPowerEmpty() {
        return currentPower <= 0;
    }
}

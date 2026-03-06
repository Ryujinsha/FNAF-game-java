package com.ryujinsha.system;

import java.util.Random;

public class RNGManager {
    private Random random;

    public RNGManager() {
        this.random = new Random();
    }

    // Menghasilkan angka acak dari 1 sampai 100
    public int roll() {
        return random.nextInt(100) + 1;
    }

    // Method canggih: Tinggal masukkan persentase probabilitas (0-100)
    // Return true jika event terjadi (roll angka <= probabilitas)
    public boolean checkEvent(int probabilityChance) {
        return roll() <= probabilityChance;
    }
}
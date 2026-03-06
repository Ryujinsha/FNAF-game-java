package com.ryujinsha.system;

public class TimeSystem {
    private int time; // 0 merepresentasikan 12 AM, 1 = 1 AM, dst.

    public TimeSystem() {
        this.time = 0; // Game selalu mulai jam 12 malam
    }

    // Dipanggil setiap kali 1 turn/loop selesai
    public void advanceTime() {
        time++;
    }

    public int getTime() {
        return time;
    }

    // Helper untuk mengubah angka integer menjadi format jam digital yang keren
    public String getFormattedTime() {
        if (time == 0) {
            return "12:00 AM";
        }
        // Menggunakan String.format agar selalu 2 digit (contoh: 01:00 AM)
        return String.format("%02d:00 AM", time);
    }

    // Pengecekan kondisi menang
    public boolean isMorning() {
        return time >= 6;
    }
}
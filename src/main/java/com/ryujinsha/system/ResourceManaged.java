package com.ryujinsha.system;

/**
 * ✨ Interface untuk komponen yang memiliki sumber daya (seperti Timer) yang perlu dibersihkan.
 */
public interface ResourceManaged {
    /**
     * Panggil ini untuk menghentikan semua proses latar belakang (Timer, dll).
     */
    void stopAllProcesses();
}

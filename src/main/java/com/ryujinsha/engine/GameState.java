package com.ryujinsha.engine;

/**
 * Mendefinisikan status alur utama permainan pada satu waktu.
 */
public enum GameState {
    PLAYING,      // Sedang bermain normal
    INCOMING,     // Babak awal/prolog
    HALLWAY,      // ✨ BARU: Fase lorong awal
    STRUGGLING,   // Sedang menahan pintu (QTE)
    LOCKPICKING,  // Sedang mencongkel gembok (QTE)
    JUMPSCARE,    // Sedang animasi jumpscare
    GAMEOVER      // Game sudah berakhir (Menang/Kalah)
}

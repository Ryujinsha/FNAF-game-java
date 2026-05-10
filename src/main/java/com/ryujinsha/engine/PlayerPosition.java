package com.ryujinsha.engine;

/**
 * Mendefinisikan posisi spasial pemain saat ini.
 */
public enum PlayerPosition {
    FRONT_ROOM, // Menghadap pintu utama/ventilasi
    PEEKING_KEYHOLE, // Mengintip lubang kunci
    PEEKING_VENT,    // Mengintip ventilasi
    BACK_ROOM,  // Menghadap pintu belakang (gembok)
    CABINET     // Sedang bersembunyi di dalam kabinet
}

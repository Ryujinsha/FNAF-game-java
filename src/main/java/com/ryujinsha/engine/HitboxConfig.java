package com.ryujinsha.engine;

import java.awt.Rectangle;

/**
 * ✨ SISTEM HITBOX TERPUSAT
 *
 * Semua koordinat hitbox game didefinisikan di sini dalam ruang logika
 * (space 2816x1536). Ini memudahkan kalibrasi tanpa harus mengubah GameGUI.
 *
 * CARA MEMBACA KOORDINAT:
 *   new Rectangle(X, Y, LEBAR, TINGGI)
 *   Semua nilai dalam unit "pixel logika" dari resolusi base 2816 x 1536
 *
 * CARA KALIBRASI:
 *   Buka file aset di image editor, ukur posisi elemen yang diinginkan.
 *   Perhatikan bahwa aset me-stretch mengisi seluruh 2816x1536 saat rendering.
 */
public class HitboxConfig {

    // ============================================================
    // RESOLUSI BASE (Cangkang Logika)
    // ============================================================
    public static final int BASE_WIDTH  = 2816;
    public static final int BASE_HEIGHT = 1536;

    // ============================================================
    // FRONT ROOM — Ruang Kantor Depan
    // ============================================================

    /**
     * Area render sprite Enemy A (The Red One) di pintu depan kiri.
     * Sprite akan di-scale ke fit lebar kotak ini, align-bottom.
     * origX, origY = posisi kotak target dalam space 2816x1536
     * origW, origH = ukuran kotak target
     */
    public static final int ENEMY_A_SPRITE_X = 480;
    public static final int ENEMY_A_SPRITE_Y = 100;
    public static final int ENEMY_A_SPRITE_W = 560;
    public static final int ENEMY_A_SPRITE_H = 1350;

    /**
     * Area render sprite Enemy B (Hina) di ventilasi kanan.
     */
    public static final int ENEMY_B_SPRITE_X = 1820;
    public static final int ENEMY_B_SPRITE_Y = 80;
    public static final int ENEMY_B_SPRITE_W = 600; // Dikoreksi agar lebih pas
    public static final int ENEMY_B_SPRITE_H = 1200;

    /**
     * Posisi Hina saat di Phase 2 (Muncul solid di depan pemain).
     * Diletakkan agak tengah agar mengagetkan.
     */
    public static final int ENEMY_B_PHASE2_X = 1100;
    public static final int ENEMY_B_PHASE2_Y = 50;
    public static final int ENEMY_B_PHASE2_W = 750;
    public static final int ENEMY_B_PHASE2_H = 1400;

    // ============================================================
    // BACK ROOM — Ruang Pintu Belakang
    // ============================================================

    /**
     * Hitbox klik untuk menyembunyikan diri ke dalam kabinet.
     * Kabinet ada di sisi KANAN bawah back room.
     * Dibuat BESAR agar mudah diklik.
     */
    public static final Rectangle CABINET_HITBOX = new Rectangle(1950, 620, 800, 916);

    /**
     * Hitbox klik untuk mulai mencongkel gembok pintu belakang.
     * Pintu gembok ada di bagian TENGAH-KIRI back room.
     * Dibuat vertikal penuh agar covering seluruh area pintu.
     */
    public static final Rectangle LOCKDOOR_HITBOX = new Rectangle(550, 80, 1100, 1400);

    // ============================================================
    // KABINET VIEW — Celah Pandangan Dari Dalam Kabinet
    // ============================================================

    /**
     * Lebar celah pandangan kabinet sebagai fraksi dari lebar game (saat idle).
     */
    public static final double CABINET_SLIT_WIDTH_FRACTION = 1.0 / 7.2;

    /** Lebar celah QTE saat pintu tertutup rapat (struggleValue 100) */
    public static final int QTE_SLIT_WIDTH_MIN = 300;

    /** Lebar celah QTE saat pintu terbuka lebar (struggleValue 0) */
    public static final int QTE_SLIT_WIDTH_MAX = 850;

    /**
     * Margin vertikal celah kabinet (atas & bawah).
     * 1/5 dari tinggi = celah dimulai 20% dari atas, berakhir 20% dari bawah.
     */
    public static final double CABINET_SLIT_MARGIN_FRACTION = 1.0 / 5.0;

    // ============================================================
    // UI BUTTONS — Tombol Floating di Layar
    // ============================================================

    /** Lebar tombol Door/Look */
    public static final int BTN_CENTER_W = 260;
    public static final int BTN_CENTER_H = 62;
    /** Margin bawah tombol tengah dari tepi panel */
    public static final int BTN_CENTER_MARGIN_BOTTOM = 35;

    /** Ukuran tombol Look Left/Right */
    public static final int BTN_EDGE_W   = 55;
    public static final int BTN_EDGE_H   = 160;
    public static final int BTN_EDGE_MARGIN_SIDE = 12;

    // ============================================================
    // SISTEM ANIMASI — QTE & RETREAT
    // ============================================================

    /** Kecepatan sliding horizontal saat retreat (pixel logika per frame) */
    public static final int RETREAT_SPEED_X = 50;
    
    /** Durasi retreat dalam jumlah update tick (kira-kira 1.5 - 2 detik) */
    public static final int RETREAT_DURATION_TICKS = 20;

    /** Offset horizontal maksimum tangan saat QTE (membuka lebar) */
    public static final int QTE_HAND_SLIDE_MAX = 150;
}

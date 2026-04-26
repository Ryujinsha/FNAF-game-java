package com.ryujinsha.system;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * ✨ SISTEM CACHE ASET GAMBAR
 *
 * Menghindari re-load gambar yang sama berkali-kali dari classpath.
 * Semua gambar di-cache setelah pertama kali dimuat.
 *
 * Cara pakai:
 *   Image img = AssetCache.get("/assets/rooms/front_room.png");
 *
 * Catatan:
 *   Cache bersifat global (static) dan bertahan sepanjang aplikasi berjalan.
 *   Panggil clearAll() jika ingin membebaskan memori (misal saat keluar game).
 */
public class AssetCache {

    /** Internal cache: path → gambar yang sudah dimuat */
    private static final Map<String, Image> cache = new HashMap<>();

    /** Referensi kelas untuk mencari resource dari classpath */
    private static final Class<?> LOADER_CLASS = AssetCache.class;

    // Constructor private — kelas ini tidak boleh di-instantiate
    private AssetCache() {}

    // ============================================================
    // PUBLIC API
    // ============================================================

    /**
     * Ambil gambar dari cache, atau muat dari classpath jika belum ada.
     *
     * @param path Path resource dari root classpath, diawali '/'
     *             Contoh: "/assets/rooms/front_room.png"
     * @return Image yang sudah dimuat, atau null jika file tidak ditemukan
     */
    public static Image get(String path) {
        if (path == null || path.isEmpty()) return null;

        // Cek cache dulu
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        // Muat dari classpath
        Image loaded = loadFromResource(path);

        // Simpan ke cache (bahkan jika null, agar tidak retry terus-menerus)
        cache.put(path, loaded);

        if (loaded == null) {
            System.err.println("[AssetCache] ⚠️ File tidak ditemukan: " + path);
        } else {
            System.out.println("[AssetCache] ✅ Loaded: " + path);
        }

        return loaded;
    }

    /**
     * Preload beberapa aset sekaligus di awal game agar tidak ada lag saat dipakai.
     * Panggil di thread startup atau saat loading screen.
     *
     * @param paths Array path aset yang ingin di-preload
     */
    public static void preload(String... paths) {
        for (String path : paths) {
            get(path); // Trigger load dan masuk cache
        }
    }

    /**
     * Hapus satu aset dari cache (paksa reload berikutnya).
     *
     * @param path Path aset yang ingin dihapus dari cache
     */
    public static void invalidate(String path) {
        cache.remove(path);
    }

    /**
     * Kosongkan seluruh cache untuk membebaskan memori.
     * Semua gambar akan dimuat ulang saat diminta lagi.
     */
    public static void clearAll() {
        cache.clear();
        System.out.println("[AssetCache] Cache dikosongkan.");
    }

    /**
     * Cek apakah sebuah aset sudah ada di cache.
     */
    public static boolean isCached(String path) {
        return cache.containsKey(path) && cache.get(path) != null;
    }

    /**
     * Jumlah aset yang saat ini ada di dalam cache.
     */
    public static int size() {
        return cache.size();
    }

    // ============================================================
    // PRIVATE HELPER
    // ============================================================

    private static Image loadFromResource(String path) {
        try {
            URL url = LOADER_CLASS.getResource(path);
            if (url != null) {
                return new ImageIcon(url).getImage();
            }
        } catch (Exception e) {
            System.err.println("[AssetCache] Error memuat " + path + ": " + e.getMessage());
        }
        return null;
    }
}

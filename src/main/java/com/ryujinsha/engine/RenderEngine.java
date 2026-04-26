package com.ryujinsha.engine;

import java.awt.*;
import java.awt.image.ImageObserver;

/**
 * ✨ SISTEM RENDERING TERPUSAT
 *
 * Menyediakan semua utilitas rendering untuk sistem "Letterbox Cangkang".
 * Semua kalkulasi skala terpusat di sini agar konsisten di seluruh game.
 *
 * Konsep "Cangkang" (Letterbox):
 *   Game dirender dalam resolusi logika BASE_WIDTH x BASE_HEIGHT.
 *   Saat window di-resize, game di-scale proportionally dan di-center
 *   di dalam panel. Area di luar game (black bars) tidak interaktif.
 */
public class RenderEngine {

    private RenderEngine() { /* Utility class — jangan di-instantiate */ }

    // ============================================================
    // 1. KALKULASI CANGKANG (GAME BOUNDS)
    // ============================================================

    /**
     * Menghitung batas area game yang akan di-render dalam sebuah panel.
     * Menggunakan letterbox (aspek ratio dipertahankan).
     *
     * @param panelW    Lebar panel actual (layar fisik)
     * @param panelH    Tinggi panel actual (layar fisik)
     * @param baseW     Lebar resolusi logika game
     * @param baseH     Tinggi resolusi logika game
     * @return Rectangle berisi posisi dan ukuran area game di dalam panel
     */
    public static Rectangle getGameBounds(int panelW, int panelH, int baseW, int baseH) {
        if (panelW <= 0 || panelH <= 0) return new Rectangle(0, 0, baseW, baseH);
        double scale = Math.min((double) panelW / baseW, (double) panelH / baseH);
        int drawW = (int) (baseW * scale);
        int drawH = (int) (baseH * scale);
        int drawX = (panelW - drawW) / 2;
        int drawY = (panelH - drawH) / 2;
        return new Rectangle(drawX, drawY, drawW, drawH);
    }

    /**
     * Overload menggunakan konstanta dari HitboxConfig (convenience method).
     */
    public static Rectangle getGameBounds(int panelW, int panelH) {
        return getGameBounds(panelW, panelH, HitboxConfig.BASE_WIDTH, HitboxConfig.BASE_HEIGHT);
    }

    // ============================================================
    // 2. FAKTOR SKALA
    // ============================================================

    /**
     * Menghitung faktor skala dari game space ke screen space.
     *
     * @param bounds Rectangle game bounds dari getGameBounds()
     * @return skala (misal 0.5 artinya 1 unit game = 0.5 pixel layar)
     */
    public static double getScale(Rectangle bounds) {
        return (double) bounds.width / HitboxConfig.BASE_WIDTH;
    }

    // ============================================================
    // 3. KONVERSI KOORDINAT
    // ============================================================

    /**
     * Konversi koordinat klik layar (screen space) → koordinat game (game space).
     * Gunakan sebelum membandingkan dengan hitbox dari HitboxConfig.
     *
     * @param screenPoint Titik klik di koordinat layar panel
     * @param bounds      Batas area game (dari getGameBounds)
     * @return Titik dalam koordinat game space (2816x1536)
     *         atau (-1,-1) jika klik di luar area game (black bar)
     */
    public static Point screenToGame(Point screenPoint, Rectangle bounds) {
        if (!bounds.contains(screenPoint)) {
            return new Point(-1, -1); // Di luar area game
        }
        double scale = getScale(bounds);
        int gameX = (int) ((screenPoint.x - bounds.x) / scale);
        int gameY = (int) ((screenPoint.y - bounds.y) / scale);
        return new Point(gameX, gameY);
    }

    /**
     * Mengecek apakah titik game space ada dalam hitbox tertentu.
     * Mengembalikan false otomatis jika gamePoint = (-1,-1) (klik di black bar).
     */
    public static boolean hitboxContains(Rectangle hitbox, Point gamePoint) {
        if (gamePoint.x < 0 || gamePoint.y < 0) return false;
        return hitbox.contains(gamePoint);
    }

    // ============================================================
    // 4. RENDERING SPRITE
    // ============================================================

    /**
     * Menggambar sprite enemy/objek di dalam cangkang game.
     *
     * Cara kerja:
     * 1. Kalkulasikan posisi kotak target di layar dari koordinat game space
     * 2. Scale sprite agar fit ke lebar kotak target
     * 3. Jika alignBottom=true, sprite menempel ke bawah kotak (FNAF style)
     *    Jika alignBottom=false, sprite menempel ke atas kotak
     *
     * @param g        Graphics context
     * @param img      Gambar yang akan di-render
     * @param bounds   Rectangle game bounds di screen
     * @param origX    Posisi X kotak target dalam game space (0..2816)
     * @param origY    Posisi Y kotak target dalam game space (0..1536)
     * @param origW    Lebar kotak target dalam game space
     * @param origH    Tinggi kotak target dalam game space
     * @param alignBottom true = sprite menempel bawah (kaki menyentuh tanah)
     * @param observer ImageObserver (biasanya panel)
     */
    public static void drawSprite(Graphics g, Image img, Rectangle bounds,
                                  int origX, int origY, int origW, int origH,
                                  boolean alignBottom, ImageObserver observer) {
        if (img == null) return;

        double scale = getScale(bounds);

        // Posisi dan ukuran kotak target di screen space
        int targetX = bounds.x + (int) (origX * scale);
        int targetY = bounds.y + (int) (origY * scale);
        int targetW = (int) (origW * scale);
        int targetH = (int) (origH * scale);

        // Dapatkan dimensi sprite asli (tunggu load)
        int imgW = img.getWidth(observer);
        int imgH = img.getHeight(observer);
        if (imgW <= 0 || imgH <= 0) return;

        // Scale sprite agar fit ke lebar kotak target (pertahankan aspek ratio)
        double imgScale = (double) targetW / imgW;
        int drawW = (int) (imgW * imgScale);
        int drawH = (int) (imgH * imgScale);

        // Posisi horizontal: center dalam kotak target
        int drawX = targetX + (targetW - drawW) / 2;

        // Posisi vertikal: bottom-align atau top-align
        int drawY = alignBottom
                ? (targetY + targetH - drawH)
                : targetY;

        g.drawImage(img, drawX, drawY, drawW, drawH, observer);
    }

    /**
     * Overload dengan Rectangle untuk origRect — convenience method.
     */
    public static void drawSprite(Graphics g, Image img, Rectangle bounds,
                                  Rectangle origRect, boolean alignBottom, ImageObserver observer) {
        drawSprite(g, img, bounds,
                origRect.x, origRect.y, origRect.width, origRect.height,
                alignBottom, observer);
    }

    // ============================================================
    // 5. HELPER RENDERING DEBUG (opsional, aktifkan saat perlu)
    // ============================================================

    /**
     * Gambar overlay hitbox di runtime untuk debugging posisi.
     * Panggil di dalam paintComponent() jika perlu kalibrasi visual.
     *
     * @param g2d      Graphics2D context
     * @param hitbox   Rectangle hitbox dalam game space
     * @param bounds   Game bounds dari getGameBounds()
     * @param color    Warna overlay (gunakan alpha agar semi-transparan)
     */
    public static void drawHitboxDebug(Graphics2D g2d, Rectangle hitbox,
                                       Rectangle bounds, Color color) {
        double scale = getScale(bounds);
        int sx = bounds.x + (int) (hitbox.x * scale);
        int sy = bounds.y + (int) (hitbox.y * scale);
        int sw = (int) (hitbox.width * scale);
        int sh = (int) (hitbox.height * scale);

        g2d.setColor(color);
        g2d.drawRect(sx, sy, sw, sh);

        // Label koordinat
        g2d.setFont(new Font("Consolas", Font.BOLD, 11));
        g2d.drawString(hitbox.x + "," + hitbox.y, sx, sy - 2);
    }
}

package com.ryujinsha.engine;

import com.ryujinsha.entity.Enemy;
import com.ryujinsha.system.AssetCache;
import java.awt.*;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

/**
 * Menangani semua rendering grafis untuk GameGUI.
 * Berperan sebagai View dalam pola MVC — membaca state dari GameContext.
 */
public class GameRenderer extends JPanel {
    private GameContext ctx;

    // Path konstanta aset ruangan
    private static final String PATH_FRONT_ROOM = "/assets/rooms/front_room.png";
    private static final String PATH_FRONT_DOOR = "/assets/rooms/front_door.png";
    private static final String PATH_BACK_DOOR = "/assets/rooms/back_door.png";
    private static final String PATH_BACK_DOOR_OPENED = "/assets/rooms/back_door_opened.png";
    private static final String PATH_HALLWAY = "/assets/rooms/hallway.png";

    public GameRenderer(GameContext ctx) {
        this.ctx = ctx;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        int pw = getWidth();
        int ph = getHeight();

        // Background hitam (letterbox)
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, pw, ph);

        // Ambil batas cangkang
        Rectangle bounds = RenderEngine.getGameBounds(pw, ph);

        if (ctx.getCurrentState() == GameState.HALLWAY) {
            paintHallway(g2d, bounds);
        } else if (ctx.getCurrentPosition() == PlayerPosition.BACK_ROOM) {
            paintBackRoom(g2d, bounds);
        } else {
            paintFrontRoom(g2d, bounds);
        }

        if (ctx.getCurrentPosition() == PlayerPosition.CABINET) {
            paintCabinetView(g2d, bounds, pw, ph);
        } else if (ctx.isPeekingKeyhole()) {
            paintKeyholeView(g2d, pw, ph);
        } else if (ctx.isPeekingVent()) {
            paintVentView(g2d, pw, ph);
        }

        if (ctx.getVignetteIntensity() > 0) {
            paintVignette(g2d, pw, ph);
        }

        // Draw darkness overlay (FNAF 4 style atmosphere)
        if (ctx.getCurrentState() != GameState.HALLWAY && ctx.getCurrentState() != GameState.GAMEOVER) {
            paintDarknessOverlay(g2d, pw, ph);
        }

        if (ctx.isFlickering()) {
            g2d.setColor(new Color(0, 0, 0, (int) (ctx.getFlickerAlpha() * 255)));
            g2d.fillRect(0, 0, pw, ph);
        }

        if (ctx.isIncomingDialogVisible()) {
            paintIncomingDialog(g2d, pw, ph);
        }

        if (ctx.isHallwayCutsceneActive()) {
            paintHallwayCutscene(g2d, pw, ph);
        }

        if (ctx.isRetreating()) {
            paintRetreatOverlay(g2d, bounds);
        }

        if (MainFrame.isDevMode) {
            paintDevLogs(g2d);
        }

        // ✨ LEADERBOARD: Timer HUD saat bermain
        if (ctx.getCurrentState() == GameState.PLAYING || ctx.getCurrentState() == GameState.STRUGGLING
                || ctx.getCurrentState() == GameState.LOCKPICKING) {
            paintTimerHUD(g2d, pw, ph);
        }
    }

    private void paintFrontRoom(Graphics2D g2d, Rectangle bounds) {
        Image imgFront = AssetCache.get(PATH_FRONT_ROOM);
        if (imgFront != null) {
            g2d.drawImage(imgFront, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        if (ctx.getPlayer().isLeftDoorClosed()) {
            Image imgDoor = AssetCache.get(PATH_FRONT_DOOR);
            if (imgDoor != null) {
                g2d.drawImage(imgDoor, bounds.x, bounds.y, bounds.width, bounds.height, this);
            }
        }
    }

    private void paintBackRoom(Graphics2D g2d, Rectangle bounds) {
        boolean isUnlocked = (ctx.getLockBars() >= 6);
        String path = isUnlocked ? PATH_BACK_DOOR_OPENED : PATH_BACK_DOOR;
        Image img = AssetCache.get(path);
        if (img != null) {
            g2d.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.CABINET_HITBOX, bounds, new Color(0, 255, 0, 180));
        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.LOCKDOOR_HITBOX, bounds, new Color(255, 0, 0, 180));
    }

    private void paintHallway(Graphics2D g2d, Rectangle bounds) {
        Image img = AssetCache.get(PATH_HALLWAY);
        if (img != null) {
            g2d.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        // Draw debug hitboxes in dev mode
        if (MainFrame.isDevMode) {
            RenderEngine.drawHitboxDebug(g2d, HitboxConfig.HALLWAY_CABINET_HITBOX, bounds, new Color(0, 255, 0, 100));
            RenderEngine.drawHitboxDebug(g2d, HitboxConfig.HALLWAY_TABLE_HITBOX, bounds, new Color(0, 255, 0, 100));
            RenderEngine.drawHitboxDebug(g2d, HitboxConfig.HALLWAY_DOOR_HITBOX, bounds, new Color(255, 0, 0, 100));
        }

        // Display inventory if key found
        if (ctx.hasHallwayKey()) {
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 24));
            g2d.drawString("[ Kunci Lorong Didapatkan ]", bounds.x + 50, bounds.y + 100);
        }
    }

    private void paintHallwayCutscene(Graphics2D g2d, int pw, int ph) {
        // Black overlay
        g2d.setColor(new Color(0, 0, 0, 230));
        g2d.fillRect(0, 0, pw, ph);

        String[] texts = ctx.getHallwayCutsceneTexts();
        int index = ctx.getHallwayCutsceneIndex();
        if (index < texts.length) {
            String text = ctx.getCurrentDisplayedText(); // ✨ TYPEWRITER TEXT
            g2d.setFont(new Font("Consolas", Font.BOLD, 28));
            g2d.setColor(Color.WHITE);
            FontMetrics fm = g2d.getFontMetrics();
            int textW = fm.stringWidth(text);
            g2d.drawString(text, (pw - textW) / 2, ph / 2);

            // Tampilkan tulisan lanjut jika animasi text selesai
            if (text.length() >= texts[index].length()) {
                g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
                g2d.setColor(Color.GRAY);
                String prompt = "[ Klik untuk Lanjut ]";
                g2d.drawString(prompt, (pw - g2d.getFontMetrics().stringWidth(prompt)) / 2, ph / 2 + 100);
            }
        }
    }

    private void paintCabinetView(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        if (ctx.getCurrentState() == GameState.STRUGGLING && ctx.getQteBodyImg() != null) {
            paintStruggleQTE(g2d, bounds, pw, ph);
        } else {
            int slitWidth = (int) (bounds.width * HitboxConfig.CABINET_SLIT_WIDTH_FRACTION);
            int slitX = bounds.x + (bounds.width - slitWidth) / 2;
            int slitMarginV = (int) (bounds.height * HitboxConfig.CABINET_SLIT_MARGIN_FRACTION);
            int slitY = bounds.y + slitMarginV;
            int slitH = bounds.height - slitMarginV * 2;

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, slitX, ph);
            g2d.fillRect(slitX + slitWidth, 0, pw - (slitX + slitWidth), ph);
            g2d.fillRect(slitX, 0, slitWidth, slitY);
            g2d.fillRect(slitX, slitY + slitH, slitWidth, ph - (slitY + slitH));

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.drawString(">>> MENGINTIP DARI DALAM KABINET <<<", 30, 40);
        }
    }

    private void paintRetreatOverlay(Graphics2D g2d, Rectangle bounds) {
        Image retreatImg = ctx.getRetreatImg();
        Enemy lastDefeatedEnemy = ctx.getLastDefeatedEnemy();
        if (retreatImg == null || lastDefeatedEnemy == null) return;

        double progress = (double) ctx.getRetreatAnimTicks() / HitboxConfig.RETREAT_DURATION_TICKS;
        double scaleFactor = 1.3 - (progress * 0.5); 
        int xOffset = ctx.getRetreatAnimTicks() * HitboxConfig.RETREAT_SPEED_X;

        int basePosX, basePosY, baseW, baseH;
        if (lastDefeatedEnemy == ctx.getEnemyB()) {
            double hinaScale = scaleFactor * 1.5;
            basePosX = HitboxConfig.ENEMY_B_PHASE2_X + xOffset;
            basePosY = HitboxConfig.ENEMY_B_PHASE2_Y;
            baseW = (int) (HitboxConfig.ENEMY_B_PHASE2_W * hinaScale);
            baseH = (int) (HitboxConfig.ENEMY_B_PHASE2_H * hinaScale);
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, this);
        } else {
            basePosX = HitboxConfig.ENEMY_A_KEYHOLE_P2_X - xOffset;
            basePosY = HitboxConfig.ENEMY_A_KEYHOLE_P2_Y;
            baseW = (int) (HitboxConfig.ENEMY_A_KEYHOLE_P2_W * scaleFactor);
            baseH = (int) (HitboxConfig.ENEMY_A_KEYHOLE_P2_H * scaleFactor);
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, this);
        }
    }

    private void paintStruggleQTE(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        double progressRatio = ctx.getStruggleValue() / 100.0;
        int slitWidth = (int) (HitboxConfig.QTE_SLIT_WIDTH_MAX - (progressRatio * (HitboxConfig.QTE_SLIT_WIDTH_MAX - HitboxConfig.QTE_SLIT_WIDTH_MIN)));

        int slitX = bounds.x + (bounds.width - slitWidth) / 2;
        int slitMarginV = (int) (bounds.height * HitboxConfig.CABINET_SLIT_MARGIN_FRACTION);
        int slitY = bounds.y + slitMarginV;
        int slitH = bounds.height - slitMarginV * 2;

        int bodyH = (int) (slitH * 1.4); 
        int bodyOrigW = ctx.getQteBodyImg().getWidth(this);
        int bodyOrigH = ctx.getQteBodyImg().getHeight(this);
        int bodyW = (bodyOrigH > 0) ? (bodyH * bodyOrigW / bodyOrigH) : slitWidth;
        int bodyX = bounds.x + (bounds.width - bodyW) / 2;
        int bodyY = slitY - (bodyH - slitH) / 2;

        g2d.drawImage(ctx.getQteBodyImg(), bodyX, bodyY, bodyW, bodyH, this);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, slitX, ph);
        g2d.fillRect(slitX + slitWidth, 0, pw - (slitX + slitWidth), ph);
        g2d.fillRect(slitX, 0, slitWidth, slitY);
        g2d.fillRect(slitX, slitY + slitH, slitWidth, ph - (slitY + slitH));

        if (ctx.getQteHandLeftImg() != null && ctx.getQteHandRightImg() != null) {
            int handH = (int) (slitH * 0.85);
            int hOrigW = ctx.getQteHandLeftImg().getWidth(this);
            int hOrigH = ctx.getQteHandLeftImg().getHeight(this);
            int handW = (hOrigH > 0) ? (handH * hOrigW / hOrigH) : (slitWidth / 2);
            int handY = slitY + (slitH - handH) / 2;

            int lx = slitX - handW + (handW / 5);
            g2d.drawImage(ctx.getQteHandLeftImg(), lx, handY, handW, handH, this);

            int rx = slitX + slitWidth - (handW / 5);
            g2d.drawImage(ctx.getQteHandRightImg(), rx, handY, handW, handH, this);
        }

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Consolas", Font.BOLD, 30));
        String warn = "!!! TAHAN PINTU !!!";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(warn, (pw - fm.stringWidth(warn)) / 2, ph / 2 - 120);

        int barW = 500, barH = 25;
        int barX = (pw - barW) / 2;
        int barY = ph - 100;
        g2d.setColor(Color.BLACK);
        g2d.fillRect(barX, barY, barW, barH);

        if (ctx.getStruggleValue() < 30) g2d.setColor(Color.RED);
        else if (ctx.getStruggleValue() < 70) g2d.setColor(Color.YELLOW);
        else g2d.setColor(Color.GREEN);
        
        g2d.fillRect(barX, barY, (int) ((progressRatio) * barW), barH);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barW, barH);

        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        String instText = "SPAM KLIK ATAU SPASI! [" + ctx.getStruggleValue() + "%]";
        FontMetrics fmT = g2d.getFontMetrics();
        g2d.drawString(instText, (pw - fmT.stringWidth(instText)) / 2, barY - 15);
    }

    private void paintVignette(Graphics2D g2d, int w, int h) {
        float alpha = Math.min(0.8f, ctx.getVignetteIntensity());
        RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f),
                Math.max(w, h) / 1.5f,
                new float[] { 0.0f, 0.8f, 1.0f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, (int) (alpha * 150)), new Color(0, 0, 0, (int) (alpha * 255)) }
        );
        g2d.setPaint(rgp);
        g2d.fillRect(0, 0, w, h);
    }

    private void paintDevLogs(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.PLAIN, 14));
        int y = 30;
        for (String log : ctx.getDevLogs()) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(log, 22, y + 2);
            g2d.setColor(Color.CYAN);
            g2d.drawString(log, 20, y);
            y += 20;
        }
    }

    private void paintIncomingDialog(Graphics2D g2d, int pw, int ph) {
        String text = "\"Baik, mari kita mulai malam yang berat ini\"";
        g2d.setFont(new Font("Consolas", Font.ITALIC, 32));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getHeight();

        int padX = 40;
        int padY = 20;
        int boxW = textW + padX * 2;
        int boxH = textH + padY * 2;
        int boxX = (pw - boxW) / 2;
        int boxY = ph - boxH - 100;

        // Background Box (Glassmorphism style)
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 20, 20);
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 20, 20);

        // Shadow Text
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, boxX + padX + 2, boxY + padY + fm.getAscent() + 2);

        // Main Text
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, boxX + padX, boxY + padY + fm.getAscent());
    }

    private void paintKeyholeView(Graphics2D g2d, int pw, int ph) {
        Rectangle bounds = RenderEngine.getGameBounds(pw, ph);
        
        // Base Background: tunnel
        Image tunnel = AssetCache.get("/assets/keyhole/tunnel.png");
        if (tunnel != null) {
            g2d.drawImage(tunnel, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        Enemy enemyA = ctx.getEnemyA();
        if (enemyA.isAtDoor() && ctx.isFlashlightOn()) {
            Image enemySprite = null;
            if (enemyA.getPatienceTimer() == 2) {
                enemySprite = AssetCache.get("/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png");
            } else if (enemyA.getPatienceTimer() == 1 || enemyA.getPatienceTimer() <= 0) {
                enemySprite = AssetCache.get("/assets/enemies/enemy_a_door/idle/the-red-idle-phase-2.png");
            }
            if (enemySprite != null) {
                int renderX, renderY, spriteW, spriteH;
                if (enemyA.getPatienceTimer() == 2) {
                    renderX = HitboxConfig.ENEMY_A_KEYHOLE_P1_X;
                    renderY = HitboxConfig.ENEMY_A_KEYHOLE_P1_Y;
                    spriteW = HitboxConfig.ENEMY_A_KEYHOLE_P1_W;
                    spriteH = HitboxConfig.ENEMY_A_KEYHOLE_P1_H;
                } else {
                    renderX = HitboxConfig.ENEMY_A_KEYHOLE_P2_X;
                    renderY = HitboxConfig.ENEMY_A_KEYHOLE_P2_Y;
                    spriteW = HitboxConfig.ENEMY_A_KEYHOLE_P2_W;
                    spriteH = HitboxConfig.ENEMY_A_KEYHOLE_P2_H;
                }
                RenderEngine.drawSprite(g2d, enemySprite, bounds,
                        renderX, renderY, spriteW, spriteH, true, this);
            }
        }

        // Draw door overlay if patience is 3 (door closed)
        if (!enemyA.isAtDoor() || enemyA.getPatienceTimer() >= 3) {
            Image tunnelDoor = AssetCache.get("/assets/keyhole/tunnel_door.png");
            if (tunnelDoor != null) {
                g2d.drawImage(tunnelDoor, bounds.x, bounds.y, bounds.width, bounds.height, this);
            }
        }

        // Letterbox / overlay to simulate looking through keyhole
        g2d.setColor(new Color(0, 0, 0, 220));
        java.awt.geom.Area outer = new java.awt.geom.Area(new Rectangle(0, 0, pw, ph));
        int holeW = (int) (bounds.width * 0.6);
        int holeH = (int) (bounds.height * 0.6);
        int holeX = bounds.x + (bounds.width - holeW) / 2;
        int holeY = bounds.y + (bounds.height - holeH) / 2;
        
        java.awt.geom.Area innerCircle = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(holeX, holeY, holeW, holeH));
        int[] tx = {bounds.x + bounds.width/2 - holeW/4, bounds.x + bounds.width/2 + holeW/4, bounds.x + bounds.width/2};
        int[] ty = {holeY + holeH/2, holeY + holeH/2, holeY + holeH + (int)(holeH*0.4)};
        java.awt.geom.Area innerTriangle = new java.awt.geom.Area(new java.awt.Polygon(tx, ty, 3));
        
        innerCircle.add(innerTriangle);
        outer.subtract(innerCircle);
        g2d.fill(outer);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        String text = ">>> MENGINTIP LUBANG KUNCI (F=Senter) <<<";
        g2d.drawString(text, (pw - g2d.getFontMetrics().stringWidth(text))/2, 50);
    }

    private void paintVentView(Graphics2D g2d, int pw, int ph) {
        Rectangle bounds = RenderEngine.getGameBounds(pw, ph);

        // Vent back
        Image ventBack = AssetCache.get("/assets/vent/vent_back.png");
        if (ventBack != null) {
            g2d.drawImage(ventBack, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }
        // Enemy B — 3-Phase Vent Rendering
        Enemy enemyB = ctx.getEnemyB();
        if (enemyB.isAtDoor() && ctx.isFlashlightOn()) {
            Image ventSprite = null;
            int renderX, renderY, spriteW, spriteH;

            if (enemyB.getPatienceTimer() == 3) {
                // Phase 1: Hina muncul dari sisi kanan lubang vent (partial)
                ventSprite = AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase_1.png");
                renderX = HitboxConfig.ENEMY_B_VENT_P1_X;
                renderY = HitboxConfig.ENEMY_B_VENT_P1_Y;
                spriteW = HitboxConfig.ENEMY_B_VENT_P1_W;
                spriteH = HitboxConfig.ENEMY_B_VENT_P1_H;
            } else {
                // Phase 2 (patience<=2): Hina memenuhi seluruh lubang vent
                ventSprite = AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
                renderX = HitboxConfig.ENEMY_B_VENT_P2_X;
                renderY = HitboxConfig.ENEMY_B_VENT_P2_Y;
                spriteW = HitboxConfig.ENEMY_B_VENT_P2_W;
                spriteH = HitboxConfig.ENEMY_B_VENT_P2_H;
            }

            if (ventSprite != null) {
                RenderEngine.drawSprite(g2d, ventSprite, bounds,
                        renderX, renderY, spriteW, spriteH, true, this);
            }
        }

        // Vent front overlay
        Image ventFront = AssetCache.get("/assets/vent/vent_front.png");
        if (ventFront != null) {
            g2d.drawImage(ventFront, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 24));
        String text = ">>> MENGECEK VENTILASI (F=Senter) <<<";
        g2d.drawString(text, (pw - g2d.getFontMetrics().stringWidth(text))/2, 50);
    }

    private void paintDarknessOverlay(Graphics2D g2d, int pw, int ph) {
        if (ctx.isFlashlightOn() && (ctx.getCurrentPosition() == PlayerPosition.FRONT_ROOM || ctx.getCurrentPosition() == PlayerPosition.BACK_ROOM || ctx.getCurrentPosition() == PlayerPosition.PEEKING_KEYHOLE || ctx.getCurrentPosition() == PlayerPosition.PEEKING_VENT)) {
            // Senter menyala: Buat gradien melingkar dari transparan ke hitam (FNAF 4 style)
            RadialGradientPaint rgp = new RadialGradientPaint(
                    new Point2D.Float(pw / 2f, ph / 2f),
                    Math.max(pw, ph) / 1.5f,
                    new float[] { 0.0f, 0.5f, 1.0f },
                    new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 200), new Color(0, 0, 0, 255) }
            );
            g2d.setPaint(rgp);
            g2d.fillRect(0, 0, pw, ph);
        } else {
            // Senter mati: Gelap gulita
            g2d.setColor(new Color(0, 0, 0, 245));
            g2d.fillRect(0, 0, pw, ph);
        }
    }

    // ============================================================
    // TIMER HUD — Menampilkan waktu bermain di pojok kanan atas
    // ============================================================
    private void paintTimerHUD(Graphics2D g2d, int pw, int ph) {
        String timeText = ctx.getFormattedElapsedTime();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Font & metrics
        Font timerFont = new Font("Consolas", Font.BOLD, 22);
        g2d.setFont(timerFont);
        FontMetrics fm = g2d.getFontMetrics();

        String label = "⏱ " + timeText;
        int textW = fm.stringWidth(label);
        int textH = fm.getHeight();

        int padX = 14;
        int padY = 8;
        int boxW = textW + padX * 2;
        int boxH = textH + padY * 2;
        int boxX = pw - boxW - 20;
        int boxY = 15;

        // Background box semi-transparan
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        // Border
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        // Timer text
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString(label, boxX + padX, boxY + padY + fm.getAscent());
    }
}


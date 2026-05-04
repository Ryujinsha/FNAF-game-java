package com.ryujinsha.engine;

import com.ryujinsha.entity.Enemy;
import com.ryujinsha.system.AssetCache;
import java.awt.*;
import java.awt.geom.Point2D;
import javax.swing.JPanel;

/**
 * Menangani semua rendering grafis untuk GameGUI.
 * Berperan sebagai View dalam pola MVC, sehingga GameGUI tidak terlalu penuh.
 */
public class GameRenderer extends JPanel {
    private GameGUI game;

    // Path konstanta aset ruangan
    private static final String PATH_FRONT_ROOM = "/assets/rooms/front_room.png";
    private static final String PATH_FRONT_DOOR = "/assets/rooms/front_door.png";
    private static final String PATH_BACK_DOOR = "/assets/rooms/back_door.png";
    private static final String PATH_BACK_DOOR_OPENED = "/assets/rooms/back_door_opened.png";

    public GameRenderer(GameGUI game) {
        this.game = game;
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

        if (game.getCurrentPosition() == PlayerPosition.BACK_ROOM) {
            paintBackRoom(g2d, bounds);
        } else {
            paintFrontRoom(g2d, bounds);
        }

        if (game.getCurrentPosition() == PlayerPosition.CABINET) {
            paintCabinetView(g2d, bounds, pw, ph);
        }

        if (game.getVignetteIntensity() > 0) {
            paintVignette(g2d, pw, ph);
        }

        if (game.isFlickering()) {
            g2d.setColor(new Color(0, 0, 0, (int) (game.getFlickerAlpha() * 255)));
            g2d.fillRect(0, 0, pw, ph);
        }

        if (game.isRetreating()) {
            paintRetreatOverlay(g2d, bounds);
        }

        if (MainFrame.isDevMode) {
            paintDevLogs(g2d);
        }
    }

    private void paintFrontRoom(Graphics2D g2d, Rectangle bounds) {
        Enemy enemyB = game.getEnemyB();
        Enemy enemyA = game.getEnemyA();

        if (enemyB.isAtDoor() && enemyB.getPatienceTimer() == 3 && game.getCurrentState() != GameState.STRUGGLING && !game.isRetreating()) {
            Image ventSprite = AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
            if (ventSprite != null) {
                RenderEngine.drawSprite(g2d, ventSprite, bounds,
                        HitboxConfig.ENEMY_B_SPRITE_X, HitboxConfig.ENEMY_B_SPRITE_Y,
                        HitboxConfig.ENEMY_B_SPRITE_W, HitboxConfig.ENEMY_B_SPRITE_H,
                        true, this);
            }
        }

        Image doorEnemyVisual = game.getDoorEnemyVisual();
        if (doorEnemyVisual != null && !game.getPlayer().isLeftDoorClosed() && !game.isFlickering()) {
            RenderEngine.drawSprite(g2d, doorEnemyVisual, bounds,
                    HitboxConfig.ENEMY_A_SPRITE_X, HitboxConfig.ENEMY_A_SPRITE_Y,
                    HitboxConfig.ENEMY_A_SPRITE_W, HitboxConfig.ENEMY_A_SPRITE_H,
                    true, this);
        }

        if (enemyB.isAtDoor() && enemyB.getPatienceTimer() <= 2 && game.getCurrentState() != GameState.STRUGGLING && !game.isRetreating() && !game.isFlickering()) {
            Image hinaImg = game.getIdleSprite(enemyB);
            if (hinaImg != null) {
                RenderEngine.drawSprite(g2d, hinaImg, bounds,
                        HitboxConfig.ENEMY_B_PHASE2_X, HitboxConfig.ENEMY_B_PHASE2_Y,
                        HitboxConfig.ENEMY_B_PHASE2_W, HitboxConfig.ENEMY_B_PHASE2_H,
                        true, this);
            }
        }

        Image imgFront = AssetCache.get(PATH_FRONT_ROOM);
        if (imgFront != null) {
            g2d.drawImage(imgFront, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        if (game.getPlayer().isLeftDoorClosed()) {
            Image imgDoor = AssetCache.get(PATH_FRONT_DOOR);
            if (imgDoor != null) {
                g2d.drawImage(imgDoor, bounds.x, bounds.y, bounds.width, bounds.height, this);
            }
        }
    }

    private void paintBackRoom(Graphics2D g2d, Rectangle bounds) {
        boolean isUnlocked = (game.getLockBars() >= 6);
        String path = isUnlocked ? PATH_BACK_DOOR_OPENED : PATH_BACK_DOOR;
        Image img = AssetCache.get(path);
        if (img != null) {
            g2d.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, this);
        }

        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.CABINET_HITBOX, bounds, new Color(0, 255, 0, 180));
        RenderEngine.drawHitboxDebug(g2d, HitboxConfig.LOCKDOOR_HITBOX, bounds, new Color(255, 0, 0, 180));
    }

    private void paintCabinetView(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        if (game.getCurrentState() == GameState.STRUGGLING && game.getQteBodyImg() != null) {
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
        Image retreatImg = game.getRetreatImg();
        Enemy lastDefeatedEnemy = game.getLastDefeatedEnemy();
        if (retreatImg == null || lastDefeatedEnemy == null) return;

        double progress = (double) game.getRetreatAnimTicks() / HitboxConfig.RETREAT_DURATION_TICKS;
        double scaleFactor = 1.3 - (progress * 0.5); 
        int xOffset = game.getRetreatAnimTicks() * HitboxConfig.RETREAT_SPEED_X;

        int basePosX, basePosY, baseW, baseH;
        if (lastDefeatedEnemy == game.getEnemyB()) {
            double hinaScale = scaleFactor * 1.5;
            basePosX = HitboxConfig.ENEMY_B_PHASE2_X + xOffset;
            basePosY = HitboxConfig.ENEMY_B_PHASE2_Y;
            baseW = (int) (HitboxConfig.ENEMY_B_PHASE2_W * hinaScale);
            baseH = (int) (HitboxConfig.ENEMY_B_PHASE2_H * hinaScale);
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, this);
        } else {
            basePosX = HitboxConfig.ENEMY_A_SPRITE_X - xOffset;
            basePosY = HitboxConfig.ENEMY_A_SPRITE_Y;
            baseW = (int) (HitboxConfig.ENEMY_A_SPRITE_W * scaleFactor);
            baseH = (int) (HitboxConfig.ENEMY_A_SPRITE_H * scaleFactor);
            RenderEngine.drawSprite(g2d, retreatImg, bounds, basePosX, basePosY - 50, baseW, baseH, true, this);
        }
    }

    private void paintStruggleQTE(Graphics2D g2d, Rectangle bounds, int pw, int ph) {
        double progressRatio = game.getStruggleValue() / 100.0;
        int slitWidth = (int) (HitboxConfig.QTE_SLIT_WIDTH_MAX - (progressRatio * (HitboxConfig.QTE_SLIT_WIDTH_MAX - HitboxConfig.QTE_SLIT_WIDTH_MIN)));

        int slitX = bounds.x + (bounds.width - slitWidth) / 2;
        int slitMarginV = (int) (bounds.height * HitboxConfig.CABINET_SLIT_MARGIN_FRACTION);
        int slitY = bounds.y + slitMarginV;
        int slitH = bounds.height - slitMarginV * 2;

        int bodyH = (int) (slitH * 1.4); 
        int bodyOrigW = game.getQteBodyImg().getWidth(this);
        int bodyOrigH = game.getQteBodyImg().getHeight(this);
        int bodyW = (bodyOrigH > 0) ? (bodyH * bodyOrigW / bodyOrigH) : slitWidth;
        int bodyX = bounds.x + (bounds.width - bodyW) / 2;
        int bodyY = slitY - (bodyH - slitH) / 2;

        g2d.drawImage(game.getQteBodyImg(), bodyX, bodyY, bodyW, bodyH, this);

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, slitX, ph);
        g2d.fillRect(slitX + slitWidth, 0, pw - (slitX + slitWidth), ph);
        g2d.fillRect(slitX, 0, slitWidth, slitY);
        g2d.fillRect(slitX, slitY + slitH, slitWidth, ph - (slitY + slitH));

        if (game.getQteHandLeftImg() != null && game.getQteHandRightImg() != null) {
            int handH = (int) (slitH * 0.85);
            int hOrigW = game.getQteHandLeftImg().getWidth(this);
            int hOrigH = game.getQteHandLeftImg().getHeight(this);
            int handW = (hOrigH > 0) ? (handH * hOrigW / hOrigH) : (slitWidth / 2);
            int handY = slitY + (slitH - handH) / 2;

            int lx = slitX - handW + (handW / 5);
            g2d.drawImage(game.getQteHandLeftImg(), lx, handY, handW, handH, this);

            int rx = slitX + slitWidth - (handW / 5);
            g2d.drawImage(game.getQteHandRightImg(), rx, handY, handW, handH, this);
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

        if (game.getStruggleValue() < 30) g2d.setColor(Color.RED);
        else if (game.getStruggleValue() < 70) g2d.setColor(Color.YELLOW);
        else g2d.setColor(Color.GREEN);
        
        g2d.fillRect(barX, barY, (int) ((progressRatio) * barW), barH);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(barX, barY, barW, barH);

        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        String instText = "SPAM KLIK ATAU SPASI! [" + game.getStruggleValue() + "%]";
        FontMetrics fmT = g2d.getFontMetrics();
        g2d.drawString(instText, (pw - fmT.stringWidth(instText)) / 2, barY - 15);
    }

    private void paintVignette(Graphics2D g2d, int w, int h) {
        float alpha = Math.min(0.8f, game.getVignetteIntensity());
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
        for (String log : game.getDevLogs()) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(log, 22, y + 2);
            g2d.setColor(Color.CYAN);
            g2d.drawString(log, 20, y);
            y += 20;
        }
    }
}

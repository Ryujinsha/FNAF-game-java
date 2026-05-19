package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AssetCache;
import com.ryujinsha.system.AudioManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * ✨ LockpickSystem — Mengelola seluruh mekanik QTE mencongkel gembok.
 *
 * Termasuk: UI popup panel, animasi indikator, logika hit/miss,
 * progress bar, dan transisi ke ending saat berhasil.
 */
public class LockpickSystem {

    private final GameContext ctx;
    private HallwaySystem hallwaySystem; // Lazy-set untuk menghindari circular constructor

    LockpickSystem(GameContext ctx) {
        this.ctx = ctx;
    }

    void setHallwaySystem(HallwaySystem hallwaySystem) {
        this.hallwaySystem = hallwaySystem;
    }

    // ============================================================
    // SETUP LOCKPICK UI
    // ============================================================

    JPanel setupLockpickUI() {
        ctx.lockpickPopupPanel = new JPanel();
        ctx.lockpickPopupPanel.setBackground(Color.BLACK);
        ctx.lockpickPopupPanel.setLayout(new BorderLayout());

        // ✨ QTE TIMING HIT PANEL
        ctx.qteRenderPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintQteBar((Graphics2D) g, getWidth(), getHeight());
            }
        };
        ctx.lockpickPopupPanel.add(ctx.qteRenderPanel, BorderLayout.CENTER);

        JButton btnClose = new JButton("MENJAUH DARI PINTU [S / ESC]");
        btnClose.setBackground(Color.DARK_GRAY);
        btnClose.setForeground(Color.WHITE);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 20));
        btnClose.setPreferredSize(new Dimension(800, 60));
        btnClose.addActionListener(e -> {
            stopQte();
            ctx.lockpickPopupPanel.setVisible(false);
        });
        ctx.lockpickPopupPanel.add(btnClose, BorderLayout.SOUTH);

        return ctx.lockpickPopupPanel;
    }

    // ============================================================
    // QTE BAR RENDERING
    // ============================================================

    private void paintQteBar(Graphics2D g2d, int pw, int ph) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, pw, ph);

        // Latar belakang lockpick
        Rectangle bounds = RenderEngine.getGameBounds(pw, ph);
        Image imgLock = AssetCache.get(GameContext.PATH_LOCK_DOOR);
        if (imgLock != null) {
            g2d.drawImage(imgLock, bounds.x, bounds.y, bounds.width, bounds.height, ctx.qteRenderPanel);
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRect(0, 0, pw, ph);
        }

        // Judul
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 28));
        String title = "M E N C O N G K E L   G E M B O K";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(title, (pw - fm.stringWidth(title)) / 2, 80);

        // Instruksi
        g2d.setFont(new Font("Consolas", Font.PLAIN, 22));
        String sub = "< Tekan [SPASI] tepat di zona HIJAU! >";
        fm = g2d.getFontMetrics();
        g2d.drawString(sub, (pw - fm.stringWidth(sub)) / 2, 130);

        // QTE BAR
        int qteBarW = Math.min(700, pw - 100);
        int qteBarH = 50;
        int qteBarX = (pw - qteBarW) / 2;
        int qteBarY = ph / 2 - 60;

        g2d.setColor(new Color(40, 40, 40, 220));
        g2d.fillRoundRect(qteBarX, qteBarY, qteBarW, qteBarH, 12, 12);

        // Zona Hijau
        double greenFraction = getGreenZoneWidth();
        int greenW = (int) (qteBarW * greenFraction);
        int greenX = qteBarX + (qteBarW - greenW) / 2;
        g2d.setColor(new Color(0, 200, 0, 160));
        g2d.fillRoundRect(greenX, qteBarY + 2, greenW, qteBarH - 4, 8, 8);
        g2d.setColor(new Color(0, 255, 0, 220));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(greenX, qteBarY + 2, greenW, qteBarH - 4, 8, 8);

        // Border bar luar
        g2d.setColor(new Color(100, 100, 100));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(qteBarX, qteBarY, qteBarW, qteBarH, 12, 12);

        // ✨ INDIKATOR
        if (ctx.qteActive) {
            int indicatorX = qteBarX + (int) (ctx.qteIndicatorPos * qteBarW);
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(4));
            g2d.drawLine(indicatorX, qteBarY - 5, indicatorX, qteBarY + qteBarH + 5);

            int[] triX = {indicatorX - 10, indicatorX + 10, indicatorX};
            int[] triY = {qteBarY - 20, qteBarY - 20, qteBarY - 5};
            g2d.setColor(Color.YELLOW);
            g2d.fillPolygon(triX, triY, 3);

            int[] triX2 = {indicatorX - 10, indicatorX + 10, indicatorX};
            int[] triY2 = {qteBarY + qteBarH + 20, qteBarY + qteBarH + 20, qteBarY + qteBarH + 5};
            g2d.fillPolygon(triX2, triY2, 3);
        }

        // Level indicator text
        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Consolas", Font.BOLD, 20));
        String levelText = "Level " + (ctx.lockBars + 1) + " / 6";
        fm = g2d.getFontMetrics();
        g2d.drawString(levelText, (pw - fm.stringWidth(levelText)) / 2, qteBarY - 35);

        // 6 BAR PROGRESS
        int barW = 80, barSpacing = 20;
        int startX = (pw - (6 * barW + 5 * barSpacing)) / 2;
        int barY = qteBarY + qteBarH + 80;
        for (int i = 0; i < 6; i++) {
            boolean filled = (i < ctx.lockBars);
            if (filled) {
                g2d.setColor(new Color(0, 220, 0, 200));
            } else if (i == ctx.lockBars) {
                g2d.setColor(new Color(80, 80, 0, 200));
            } else {
                g2d.setColor(new Color(50, 50, 50, 200));
            }
            g2d.fillRoundRect(startX + i * (barW + barSpacing), barY, barW, 50, 8, 8);

            if (filled) {
                g2d.setColor(new Color(100, 255, 100));
            } else if (i == ctx.lockBars) {
                g2d.setColor(Color.YELLOW);
            } else {
                g2d.setColor(Color.GRAY);
            }
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(startX + i * (barW + barSpacing), barY, barW, 50, 8, 8);

            g2d.setFont(new Font("Consolas", Font.BOLD, 18));
            fm = g2d.getFontMetrics();
            String num = String.valueOf(i + 1);
            int textX = startX + i * (barW + barSpacing) + (barW - fm.stringWidth(num)) / 2;
            int textY = barY + 32;
            g2d.setColor(filled ? Color.WHITE : Color.GRAY);
            g2d.drawString(num, textX, textY);
        }
    }

    // ============================================================
    // QTE TIMING HIT SYSTEM
    // ============================================================

    double getGreenZoneWidth() {
        return 0.35 - (ctx.lockBars * 0.04);
    }

    private double getQteSpeed() {
        return 0.015 + (ctx.lockBars * 0.004);
    }

    void startQte() {
        if (ctx.currentState == GameState.INCOMING && hallwaySystem != null) {
            hallwaySystem.switchToActualGame();
        }

        ctx.qteIndicatorPos = 0.0;
        ctx.qteDirection = 1;
        ctx.qteActive = true;

        if (ctx.qteAnimTimer != null && ctx.qteAnimTimer.isRunning())
            ctx.qteAnimTimer.stop();

        ctx.qteAnimTimer = new Timer(16, e -> {
            if (!ctx.qteActive || (ctx.currentState == GameState.GAMEOVER)) {
                ((Timer) e.getSource()).stop();
                return;
            }
            ctx.qteIndicatorPos += getQteSpeed() * ctx.qteDirection;

            if (ctx.qteIndicatorPos >= 1.0) {
                ctx.qteIndicatorPos = 1.0;
                ctx.qteDirection = -1;
            } else if (ctx.qteIndicatorPos <= 0.0) {
                ctx.qteIndicatorPos = 0.0;
                ctx.qteDirection = 1;
            }

            if (ctx.qteRenderPanel != null)
                ctx.qteRenderPanel.repaint();
        });
        ctx.qteAnimTimer.start();
    }

    void stopQte() {
        ctx.qteActive = false;
        if (ctx.qteAnimTimer != null && ctx.qteAnimTimer.isRunning())
            ctx.qteAnimTimer.stop();
    }

    void handleQteHit() {
        if (!ctx.qteActive || ctx.lockBars >= 6 || (ctx.currentState == GameState.GAMEOVER))
            return;

        double greenWidth = getGreenZoneWidth();
        double greenStart = 0.5 - greenWidth / 2;
        double greenEnd = 0.5 + greenWidth / 2;

        if (ctx.qteIndicatorPos >= greenStart && ctx.qteIndicatorPos <= greenEnd) {
            ctx.lockBars++;
            ctx.logEvent("✅ [QTE HIT] Bar " + ctx.lockBars + "/6 berhasil!");
            AudioManager.playSound("/assets/audio/sfx/button_click.wav");

            if (ctx.lockBars >= 6) {
                stopQte();
                handleLockpickSuccess();
            } else {
                ctx.qteIndicatorPos = 0.0;
                ctx.qteDirection = 1;
            }
        } else {
            ctx.lockBars = Math.max(0, ctx.lockBars - 1);
            ctx.logEvent("❌ [QTE MISS] Lockpick terpeleset! Mundur ke bar " + ctx.lockBars + "/6.");
            AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            ctx.qteIndicatorPos = 0.0;
            ctx.qteDirection = 1;
        }

        if (ctx.qteRenderPanel != null)
            ctx.qteRenderPanel.repaint();
    }

    private void handleLockpickSuccess() {
        ctx.currentState = GameState.GAMEOVER;
        ctx.gameLoopTimer.stop();
        if (ctx.lockDrainTimer != null)
            ctx.lockDrainTimer.stop();

        // ✨ LEADERBOARD: Stop timer & simpan skor
        ctx.gameEndTimeMs = System.currentTimeMillis();

        ctx.logEvent("✅ [VICTORY] Gembok berhasil dirusak! Rantai terlepas...");
        AudioManager.playSound("/assets/audio/sfx/door_open.wav");
        ctx.officePanel.repaint();

        // Simpan skor ke database (muncul dialog input nama)
        ScoreSaver.showNameInputAndSave(ctx, "VICTORY");

        Timer winDelay = new Timer(2000, evt -> ctx.mainFrame.fadeOutToScreen("ENDING"));
        winDelay.setRepeats(false);
        winDelay.start();
    }
}

package com.ryujinsha.engine;

import com.ryujinsha.system.AudioManager;

import javax.swing.*;
import java.awt.*;

/**
 * ✨ HallwaySystem — Mengelola fase lorong awal dan cutscene transisi.
 *
 * Termasuk: hallway interactions, cutscene typewriter effect,
 * incoming prolog, dan transisi ke game asli.
 */
public class HallwaySystem {

    private final GameContext ctx;
    private InputHandler inputHandler;
    private JPanel gamePanel; // Reference ke GameGUI panel untuk requestFocusInWindow

    HallwaySystem(GameContext ctx) {
        this.ctx = ctx;
    }

    void setInputHandler(InputHandler inputHandler, JPanel gamePanel) {
        this.inputHandler = inputHandler;
        this.gamePanel = gamePanel;
    }

    // ============================================================
    // START GAME (ENTRY POINT)
    // ============================================================

    void startGame() {
        // ✨ FIX: Pastikan panel mendapat focus dan UI diperbarui setelah transisi
        if (gamePanel != null) gamePanel.requestFocusInWindow();
        if (inputHandler != null) inputHandler.updateUIVisibility();

        if (ctx.currentState == GameState.INCOMING) {
            ctx.incomingDialogVisible = true;
            ctx.logEvent("🎬 [INCOMING] Prolog dimulai...");

            ctx.incomingTimer = new Timer(7000, e -> switchToActualGame());
            ctx.incomingTimer.setRepeats(false);
            ctx.incomingTimer.start();
        } else {
            ctx.gameLoopTimer.start();
            if (ctx.lockDrainTimer != null)
                ctx.lockDrainTimer.start();
        }
    }

    /**
     * Transisi dari babak Incoming ke Actual Game (PLAYING).
     */
    void switchToActualGame() {
        if (ctx.currentState != GameState.INCOMING) return;

        if (ctx.incomingTimer != null && ctx.incomingTimer.isRunning())
            ctx.incomingTimer.stop();

        ctx.currentState = GameState.PLAYING;
        ctx.areEnemiesActive = true;
        ctx.incomingDialogVisible = false;

        // ✨ LEADERBOARD: Mulai hitung waktu bermain
        ctx.gameStartTimeMs = System.currentTimeMillis();
        ctx.gameEndTimeMs = 0;

        ctx.logEvent("🎮 [ACTUAL GAME] Shift malam dimulai! Musuh aktif.");

        if (!ctx.gameLoopTimer.isRunning()) ctx.gameLoopTimer.start();
        if (!ctx.lockDrainTimer.isRunning()) ctx.lockDrainTimer.start();

        // ✨ FIX: Update UI visibility setelah transisi ke PLAYING
        if (inputHandler != null) inputHandler.updateUIVisibility();

        // ✨ BARU: Sembunyikan tombol Q/E selama 3 detik, baru muncul
        if (ctx.btnPeekKeyhole != null) ctx.btnPeekKeyhole.setVisible(false);
        if (ctx.btnPeekVent != null) ctx.btnPeekVent.setVisible(false);

        Timer peekDelayTimer = new Timer(3000, e -> {
            if (ctx.currentState == GameState.PLAYING && inputHandler != null) {
                inputHandler.updateUIVisibility();
            }
        });
        peekDelayTimer.setRepeats(false);
        peekDelayTimer.start();

        ctx.officePanel.repaint();
    }

    // ============================================================
    // HALLWAY INTERACTION
    // ============================================================

    void handleHallwayClick(Point gamePoint) {
        if (RenderEngine.hitboxContains(HitboxConfig.HALLWAY_CABINET_HITBOX, gamePoint) ||
            RenderEngine.hitboxContains(HitboxConfig.HALLWAY_TABLE_HITBOX, gamePoint)) {
            if (!ctx.hasHallwayKey) {
                ctx.hasHallwayKey = true;
                ctx.logEvent("🗝️ [ITEM] Kamu menemukan kunci!");
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
            } else {
                ctx.logEvent("🔍 Kamu sudah mengambil kunci dari sini.");
            }
        } else if (RenderEngine.hitboxContains(HitboxConfig.HALLWAY_DOOR_HITBOX, gamePoint)) {
            if (ctx.hasHallwayKey) {
                ctx.logEvent("🚪 [INTERACT] Membuka pintu besar...");
                AudioManager.playSound("/assets/audio/sfx/door_open.wav");
                startHallwayCutscene();
            } else {
                ctx.logEvent("🔒 Pintu terkunci. Kamu butuh kunci.");
            }
        }
        ctx.officePanel.repaint();
    }

    // ============================================================
    // CUTSCENE SYSTEM
    // ============================================================

    private void startHallwayCutscene() {
        ctx.hallwayCutsceneActive = true;
        ctx.hallwayCutsceneIndex = 0;
        ctx.incomingDialogVisible = false;
        startTypingText();
        ctx.officePanel.repaint();
    }

    void advanceHallwayCutscene() {
        if (ctx.typingTimer != null && ctx.typingTimer.isRunning()) {
            ctx.typingTimer.stop();
            ctx.currentDisplayedText = ctx.hallwayCutsceneTexts[ctx.hallwayCutsceneIndex];
        } else {
            ctx.hallwayCutsceneIndex++;
            if (ctx.hallwayCutsceneIndex >= ctx.hallwayCutsceneTexts.length) {
                ctx.hallwayCutsceneActive = false;
                ctx.currentState = GameState.INCOMING;
                startGame();
            } else {
                startTypingText();
            }
        }
        ctx.officePanel.repaint();
    }

    private void startTypingText() {
        if (ctx.typingTimer != null && ctx.typingTimer.isRunning()) ctx.typingTimer.stop();
        ctx.currentDisplayedText = "";
        ctx.typingCharIndex = 0;
        String targetText = ctx.hallwayCutsceneTexts[ctx.hallwayCutsceneIndex];

        ctx.typingTimer = new Timer(50, e -> {
            if (ctx.typingCharIndex < targetText.length()) {
                ctx.currentDisplayedText += targetText.charAt(ctx.typingCharIndex);
                ctx.typingCharIndex++;
                ctx.officePanel.repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        ctx.typingTimer.start();
    }
}

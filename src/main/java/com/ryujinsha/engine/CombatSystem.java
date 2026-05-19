package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AssetCache;
import com.ryujinsha.system.AudioManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * ✨ CombatSystem — Mengelola pertarungan, defense, dan efek visual.
 *
 * Termasuk: checkDoorDefense, struggle QTE, retreat animation,
 * jumpscare sequence, flicker effect, dan enemy visual management.
 */
public class CombatSystem {

    private final GameContext ctx;

    CombatSystem(GameContext ctx) {
        this.ctx = ctx;
    }

    // ============================================================
    // LOGIKA DEFENSE & SERANGAN ENEMY
    // ============================================================

    void checkDoorDefense(Enemy enemy) {
        if (!enemy.isAtDoor())
            return;

        // ENEMY A (The Red One)
        if (enemy == ctx.enemyA) {
            checkEnemyADefense(enemy);
            return;
        }

        // ENEMY B (Hina)
        checkEnemyBDefense(enemy);
    }

    private void checkEnemyADefense(Enemy enemy) {
        if (enemy.getPatienceTimer() == 3) {
            if (!ctx.hasFlickeredForEnemyA) {
                startFlickerEffect();
                ctx.hasFlickeredForEnemyA = true;
            }
            if (!ctx.isFlickering && ctx.player.isLeftDoorClosed()) {
                ctx.logEvent("🚪 *KREK*... Pintu tunnel terbuka. (Phase Start)");
                ctx.player.setLeftDoorClosed(false);
                AudioManager.playSound("/assets/audio/sfx/door_open.wav");
            }
        }
        if (enemy.getPatienceTimer() == 2) {
            ctx.logEvent("👣 *Tap tap tap*... Terdengar langkah kaki mendekat.");
            AudioManager.playSound("/assets/audio/sfx/footsteps.wav");
        }
        if (enemy.getPatienceTimer() == 1) {
            if (!ctx.hasFlickeredForDanger) {
                ctx.logEvent("👣 *TAP TAP*... Langkah kaki berhenti di dekat lubang kunci!");
                AudioManager.playSound("/assets/audio/sfx/footsteps.wav");
                startFlickerEffect();
                ctx.hasFlickeredForDanger = true;
            }
        }
        if (enemy.getPatienceTimer() <= 0) {
            handleEnemyAttack(enemy);
        }
    }

    private void checkEnemyBDefense(Enemy enemy) {
        if (enemy.getPatienceTimer() == 4) {
            ctx.logEvent("💨 *suara merangkak*... Ada sesuatu bergerak di dalam ventilasi.");
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        }
        if (enemy.getPatienceTimer() == 3) {
            ctx.logEvent("🕷️ Hina muncul di ventilasi sisi kanan...");
            updateDoorVisuals();
        }
        if (enemy.getPatienceTimer() == 2) {
            if (!ctx.hasFlickeredForPhase2) {
                startFlickerEffect();
                ctx.hasFlickeredForPhase2 = true;
            }
            ctx.logEvent("👁️ Hina berpindah ke tengah ventilasi! Dia menatapmu!");
            AudioManager.playSound("/assets/audio/sfx/door_close.wav");

            if (ctx.currentPosition == PlayerPosition.PEEKING_VENT) {
                ctx.logEvent("👤 [SAFE] Kamu memergoki Hina di ventilasi! Dia mundur.");
                AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                startRetreatAnimation(enemy);
            }
            updateDoorVisuals();
        }
        if (enemy.getPatienceTimer() == 1) {
            if (!ctx.hasFlickeredForDanger) {
                startFlickerEffect();
                ctx.hasFlickeredForDanger = true;
            }
        }
        if (enemy.getPatienceTimer() <= 0) {
            handleEnemyAttack(enemy);
        }
    }

    private void handleEnemyAttack(Enemy enemy) {
        if (ctx.currentPosition == PlayerPosition.CABINET) {
            if (ctx.hidEarly) {
                ctx.logEvent("👤 [SAFE] Kamu bersembunyi sebelum " + enemy.getName() + " mendekat. Ia menyerah.");
                AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                startRetreatAnimation(enemy);
                ctx.hidEarly = false;
            } else {
                startStruggle(enemy);
            }
        } else {
            initiateJumpscareSequence(enemy);
        }
    }

    // ============================================================
    // SISTEM FLICKER
    // ============================================================

    void startFlickerEffect() {
        ctx.isFlickering = true;
        ctx.flickerAlpha = 0f;
        AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");

        if (ctx.flickerTimer != null && ctx.flickerTimer.isRunning())
            ctx.flickerTimer.stop();

        ctx.flickerTimer = new Timer(50, new java.awt.event.ActionListener() {
            private int ticks = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                ticks++;
                if (Math.random() > 0.5) {
                    ctx.flickerAlpha = (float) (Math.random() * 0.7f);
                } else {
                    ctx.flickerAlpha = 0f;
                }
                if (ticks >= 20) {
                    ((Timer) e.getSource()).stop();
                    ctx.isFlickering = false;
                    ctx.flickerAlpha = 0f;
                }
                ctx.officePanel.repaint();
            }
        });
        ctx.flickerTimer.start();
    }

    // ============================================================
    // STRUGGLE QTE SYSTEM
    // ============================================================

    void startStruggle(Enemy enemy) {
        if (ctx.currentState == GameState.STRUGGLING)
            return;

        if (!loadQteAssets(enemy)) {
            ctx.logEvent("❌ Gagal load asset QTE untuk " + enemy.getName() + "! Jumpscare paksa.");
            initiateJumpscareSequence(enemy);
            return;
        }

        ctx.currentState = GameState.STRUGGLING;
        ctx.currentAttacker = enemy;
        ctx.struggleValue = 40;
        ctx.struggleAnimCounter = 0;

        ctx.logEvent("⚠️ " + enemy.getName() + " MENEMUKANMU DAN MENARIK PINTU KABINET! TAHAN!");
        AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");

        ctx.doorEnemyVisual = null;
        ctx.ventEnemyVisual = null;
        ctx.officePanel.repaint();

        ctx.struggleTimer = new Timer(100, e -> {
            if (!(ctx.currentState == GameState.STRUGGLING) || (ctx.currentState == GameState.GAMEOVER)) {
                ((Timer) e.getSource()).stop();
                return;
            }
            ctx.struggleValue -= 5;
            ctx.struggleAnimCounter++;

            if (ctx.struggleValue <= 0) {
                ((Timer) e.getSource()).stop();
                ctx.currentState = GameState.GAMEOVER;
                ctx.gameLoopTimer.stop();
                if (ctx.lockDrainTimer != null) ctx.lockDrainTimer.stop();

                ctx.btnDoor.setVisible(false);
                ctx.btnLookLeft.setVisible(false);
                ctx.btnLookRight.setVisible(false);
                ctx.lockpickPopupPanel.setVisible(false);

                triggerJumpscare(ctx.currentAttacker);
                return;
            }
            ctx.officePanel.repaint();
        });
        ctx.struggleTimer.start();
    }

    void checkStruggleWin() {
        if ((ctx.currentState == GameState.STRUGGLING) && ctx.struggleValue >= 100) {
            if (ctx.struggleTimer != null)
                ctx.struggleTimer.stop();
            ctx.currentState = GameState.PLAYING;
            ctx.struggleValue = 100;
            ctx.logEvent("👤 [SAFE] Kamu berhasil menahan pintunya! " +
                    ctx.currentAttacker.getName() + " menyerah dan pergi.");
            AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");

            startRetreatAnimation(ctx.currentAttacker);
            ctx.hidEarly = false;
            updateDoorVisuals();
            ctx.officePanel.repaint();
        }
    }

    // ============================================================
    // RETREAT ANIMATION SYSTEM
    // ============================================================

    void startRetreatAnimation(Enemy enemy) {
        if (ctx.isRetreating)
            return;

        ctx.lastDefeatedEnemy = enemy;
        ctx.isRetreating = true;
        ctx.retreatAnimTicks = 0;
        ctx.retreatImg = getIdleSprite(enemy);

        if (ctx.retreatTimer != null && ctx.retreatTimer.isRunning())
            ctx.retreatTimer.stop();

        ctx.retreatTimer = new Timer(50, e -> {
            ctx.retreatAnimTicks++;
            if (ctx.retreatAnimTicks >= HitboxConfig.RETREAT_DURATION_TICKS) {
                ((Timer) e.getSource()).stop();
                finishRetreat();
            }
            ctx.officePanel.repaint();
        });
        ctx.retreatTimer.start();
    }

    private void finishRetreat() {
        if (ctx.lastDefeatedEnemy != null) {
            ctx.lastDefeatedEnemy.retreat(10);
            if (ctx.lastDefeatedEnemy == ctx.enemyB) {
                ctx.hasFlickeredForPhase2 = false;
            }
            if (ctx.lastDefeatedEnemy == ctx.enemyA) {
                ctx.hasFlickeredForEnemyA = false;
            }
            ctx.hasFlickeredForDanger = false;
        }
        ctx.isRetreating = false;
        ctx.lastDefeatedEnemy = null;
        ctx.retreatImg = null;
        updateDoorVisuals();
        ctx.officePanel.repaint();
    }

    // ============================================================
    // JUMPSCARE SYSTEM
    // ============================================================

    void initiateJumpscareSequence(Enemy enemy) {
        if (ctx.currentState == GameState.GAMEOVER)
            return;
        ctx.currentState = GameState.GAMEOVER;
        ctx.gameLoopTimer.stop();
        if (ctx.lockDrainTimer != null)
            ctx.lockDrainTimer.stop();
        ctx.lockpickPopupPanel.setVisible(false);

        ctx.btnDoor.setVisible(false);
        ctx.btnLookLeft.setVisible(false);
        ctx.btnLookRight.setVisible(false);

        ctx.currentPosition = PlayerPosition.FRONT_ROOM;

        if (ctx.player.isLeftDoorClosed() && enemy == ctx.enemyA) {
            ctx.logEvent("💥 *BAM!* Pintu depan didobrak paksa!");
            ctx.player.toggleLeftDoor();
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        } else {
            AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav");
        }

        updateDoorVisuals();
        ctx.officePanel.repaint();

        Timer pauseTimer = new Timer(600, e1 -> {
            startFlickerEffect();
            Timer jumpTimer = new Timer(1200, e2 -> triggerJumpscare(enemy));
            jumpTimer.setRepeats(false);
            jumpTimer.start();
        });
        pauseTimer.setRepeats(false);
        pauseTimer.start();
    }

    private void triggerJumpscare(Enemy enemy) {
        ctx.doorEnemyVisual = null;
        ctx.ventEnemyVisual = null;
        ctx.officePanel.repaint();

        AudioManager.stopAllSounds();
        AudioManager.playSound("/assets/audio/sfx/jumpscare_scream.wav");

        String imagePath;
        if (enemy == ctx.enemyA) {
            imagePath = "/assets/enemies/enemy_a_door/jumpscare/the-red-jumpscare.gif";
        } else {
            imagePath = "/assets/enemies/enemy_b_vent/jumpscare/the-red-jumpscare.gif";
        }

        JPanel jumpscarePanel = new JPanel() {
            private final Image jsImage;
            {
                setOpaque(false);
                URL imgUrl = getClass().getResource(imagePath);
                jsImage = (imgUrl != null) ? new javax.swing.ImageIcon(imgUrl).getImage() : null;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (jsImage != null) {
                    Rectangle b = RenderEngine.getGameBounds(getWidth(), getHeight());
                    g.drawImage(jsImage, b.x, b.y, b.width, b.height, this);
                }
            }
        };

        jumpscarePanel.setBounds(0, 0, ctx.layeredPane.getWidth(), ctx.layeredPane.getHeight());
        ctx.layeredPane.add(jumpscarePanel, JLayeredPane.DRAG_LAYER);
        ctx.layeredPane.revalidate();
        ctx.layeredPane.repaint();

        Timer delayTimer = new Timer(1200, e -> {
            ctx.layeredPane.remove(jumpscarePanel);
            ctx.layeredPane.revalidate();
            ctx.layeredPane.repaint();
            endGame("GAME OVER", "Kamu diterkam oleh " + enemy.getName(), Color.RED, enemy);
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    // ============================================================
    // END GAME
    // ============================================================

    void endGame(String title, String msg, Color titleColor, Enemy killer) {
        if (!(ctx.currentState == GameState.GAMEOVER))
            ctx.currentState = GameState.GAMEOVER;
        ctx.gameLoopTimer.stop();
        if (ctx.lockDrainTimer != null)
            ctx.lockDrainTimer.stop();
        ctx.lockpickPopupPanel.setVisible(false);

        // ✨ LEADERBOARD: Stop timer & simpan skor (DEFEAT)
        ctx.gameEndTimeMs = System.currentTimeMillis();
        ScoreSaver.showNameInputAndSave(ctx, "DEFEAT");

        ctx.endTitleLabel.setText(title);
        ctx.endTitleLabel.setForeground(titleColor);
        ctx.endMessageLabel.setText(msg);

        ctx.endScreenPanel.setBounds(0, 0, ctx.layeredPane.getWidth(), ctx.layeredPane.getHeight());
        ctx.endScreenPanel.setVisible(true);
        ctx.endScreenPanel.revalidate();
        ctx.endScreenPanel.repaint();

        if (killer != null && killer.getQuotePath() != null) {
            ctx.quoteTimer = new Timer(1500, e -> AudioManager.playSound(killer.getQuotePath()));
            ctx.quoteTimer.setRepeats(false);
            ctx.quoteTimer.start();
        }
    }

    // ============================================================
    // ENEMY ASSET MANAGEMENT
    // ============================================================

    boolean loadQteAssets(Enemy enemy) {
        if (enemy == ctx.enemyA) {
            String base = "/assets/enemies/enemy_a_door/qte-state/";
            ctx.qteBodyImg = AssetCache.get(base + "the-red-one-body.png");
            ctx.qteHandLeftImg = AssetCache.get(base + "the-red-one-left-hand.png");
            ctx.qteHandRightImg = AssetCache.get(base + "the-red-one-right-hand.png");
        } else if (enemy == ctx.enemyB) {
            String base = "/assets/enemies/enemy_b_vent/qte-state/";
            ctx.qteBodyImg = AssetCache.get(base + "hina_body_qte.png");
            ctx.qteHandLeftImg = AssetCache.get(base + "hina_left_hand_qte.png");
            ctx.qteHandRightImg = AssetCache.get(base + "hina_right_hand_qte.png");
        } else {
            return false;
        }
        return (ctx.qteBodyImg != null && ctx.qteHandLeftImg != null && ctx.qteHandRightImg != null);
    }

    Image getIdleSprite(Enemy enemy) {
        if (enemy == ctx.enemyA) {
            return AssetCache.get("/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png");
        } else if (enemy == ctx.enemyB) {
            return AssetCache.get("/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
        }
        return null;
    }

    void updateDoorVisuals() {
        ctx.doorEnemyVisual = null;
        ctx.ventEnemyVisual = null;
        Enemy enemy = ctx.getEnemyAtDoor();

        if (enemy != null && enemy == ctx.enemyA && enemy.getPatienceTimer() <= 3
                && !(ctx.currentState == GameState.STRUGGLING) && !ctx.isRetreating) {
            ctx.doorEnemyVisual = getIdleSprite(enemy);
        }
        ctx.officePanel.repaint();
    }
}

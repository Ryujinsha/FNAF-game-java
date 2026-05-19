package com.ryujinsha.engine;

import com.ryujinsha.entity.Enemy;
import com.ryujinsha.system.AudioManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ✨ InputHandler — Menangani semua input keyboard dan mouse.
 *
 * Termasuk: key bindings (F11, SPACE, A, D, Q, E, F, W, S, ESC),
 * mouse click interactions, cabinet enter/exit, dan UI visibility.
 */
public class InputHandler {

    private final GameContext ctx;
    private CombatSystem combatSystem;
    private LockpickSystem lockpickSystem;
    private HallwaySystem hallwaySystem;

    InputHandler(GameContext ctx) {
        this.ctx = ctx;
    }

    void setSystems(CombatSystem combatSystem, LockpickSystem lockpickSystem, HallwaySystem hallwaySystem) {
        this.combatSystem = combatSystem;
        this.lockpickSystem = lockpickSystem;
        this.hallwaySystem = hallwaySystem;
    }

    // ============================================================
    // SETUP MOUSE INTERACTION
    // ============================================================

    void setupInteractionHits(JPanel targetPanel) {
        targetPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (ctx.currentState == GameState.GAMEOVER) return;
                targetPanel.requestFocusInWindow();

                // Prioritas 1: Struggle QTE
                if (ctx.currentState == GameState.STRUGGLING) {
                    ctx.struggleValue += 15;
                    if (ctx.struggleValue >= 100)
                        combatSystem.checkStruggleWin();
                    else {
                        ctx.struggleAnimCounter += 2;
                        ctx.officePanel.repaint();
                    }
                    return;
                }

                // Prioritas 2: Keluar kabinet
                if (ctx.currentPosition == PlayerPosition.CABINET) {
                    exitCabinet();
                    return;
                }

                // Konversi koordinat
                Rectangle bounds = RenderEngine.getGameBounds(
                        ctx.officePanel.getWidth(), ctx.officePanel.getHeight());
                Point gamePoint = RenderEngine.screenToGame(e.getPoint(), bounds);
                if (gamePoint.x < 0) return;

                // Prioritas 2.1: Hallway
                if (ctx.currentState == GameState.HALLWAY) {
                    if (ctx.incomingDialogVisible) {
                        ctx.incomingDialogVisible = false;
                        ctx.officePanel.repaint();
                        return;
                    }
                    if (ctx.hallwayCutsceneActive) {
                        hallwaySystem.advanceHallwayCutscene();
                    } else {
                        hallwaySystem.handleHallwayClick(gamePoint);
                    }
                    return;
                }
            }
        });
    }

    void handleBackRoomClick(Point gamePoint) {
        if (RenderEngine.hitboxContains(HitboxConfig.CABINET_HITBOX, gamePoint)) {
            enterCabinet();
            return;
        }
        if (RenderEngine.hitboxContains(HitboxConfig.LOCKDOOR_HITBOX, gamePoint)) {
            ctx.logEvent("🔧 [INTERACT] Mendekat ke gembok untuk mencongkel...");
            ctx.lockpickPopupPanel.setVisible(true);
            lockpickSystem.startQte();
            updateUIVisibility();
        }
    }

    // ============================================================
    // CABINET
    // ============================================================

    void enterCabinet() {
        ctx.currentPosition = PlayerPosition.CABINET;
        Enemy attacker = ctx.getEnemyAtDoor();
        if (attacker != null) {
            ctx.hidEarly = (attacker.getPatienceTimer() >= 3);
        } else {
            ctx.hidEarly = true;
        }
        AudioManager.playSound("/assets/audio/sfx/door_close.wav");
        ctx.logEvent("🚪 [HIDE] Kamu meringkuk masuk ke kabinet.");
        updateUIVisibility();
        ctx.officePanel.repaint();
    }

    void exitCabinet() {
        ctx.currentPosition = PlayerPosition.BACK_ROOM;
        AudioManager.playSound("/assets/audio/sfx/door_open.wav");
        ctx.logEvent("🚪 Kamu merangkak keluar dari kabinet.");
        updateUIVisibility();
        ctx.officePanel.repaint();
    }

    // ============================================================
    // KEY BINDINGS
    // ============================================================

    void setupKeyBindings(JPanel panel) {
        InputMap im = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = panel.getActionMap();

        // F11 — Toggle fullscreen
        im.put(KeyStroke.getKeyStroke("F11"), "toggleMaximize");
        am.put("toggleMaximize", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(panel);
                if (frame != null) {
                    int state = frame.getExtendedState();
                    frame.setExtendedState(
                            state == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
                }
            }
        });

        // SPACE — QTE Lockpick Hit + Spam Struggle
        im.put(KeyStroke.getKeyStroke("SPACE"), "spamSpace");
        am.put("spamSpace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER) return;
                if (ctx.qteActive && ctx.lockpickPopupPanel.isVisible()) {
                    lockpickSystem.handleQteHit();
                    return;
                }
                if (ctx.currentState == GameState.STRUGGLING) {
                    ctx.struggleValue += 15;
                    if (ctx.struggleValue >= 100)
                        combatSystem.checkStruggleWin();
                    else {
                        ctx.struggleAnimCounter += 2;
                        ctx.officePanel.repaint();
                    }
                }
            }
        });

        // A — Lihat kiri
        im.put(KeyStroke.getKeyStroke("A"), "lookLeft");
        am.put("lookLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.btnLookLeft.isVisible() && ctx.btnLookLeft.isEnabled())
                    ctx.btnLookLeft.doClick();
            }
        });

        // D — Lihat kanan
        im.put(KeyStroke.getKeyStroke("D"), "lookRight");
        am.put("lookRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.btnLookRight.isVisible() && ctx.btnLookRight.isEnabled())
                    ctx.btnLookRight.doClick();
            }
        });

        // Q — Mengintip lubang kunci
        im.put(KeyStroke.getKeyStroke("Q"), "peekKeyhole");
        am.put("peekKeyhole", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER ||
                    (ctx.currentPosition != PlayerPosition.FRONT_ROOM &&
                     ctx.currentPosition != PlayerPosition.PEEKING_KEYHOLE)) return;

                if (ctx.currentPosition == PlayerPosition.PEEKING_KEYHOLE) {
                    ctx.currentPosition = PlayerPosition.FRONT_ROOM;
                    ctx.logEvent("👀 Kamu berhenti mengintip lubang kunci.");
                    if (ctx.enemyA.isAtDoor() && (ctx.enemyA.getPatienceTimer() == 2 || ctx.enemyA.getPatienceTimer() == 1)) {
                        ctx.logEvent("👤 [SAFE] Kamu berhenti mengintip. The Red One pergi.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        combatSystem.startRetreatAnimation(ctx.enemyA);
                    }
                } else {
                    ctx.currentPosition = PlayerPosition.PEEKING_KEYHOLE;
                    ctx.logEvent("👀 Kamu mengintip lubang kunci...");
                }
                ctx.officePanel.repaint();
            }
        });

        // E — Mengintip ventilasi
        im.put(KeyStroke.getKeyStroke("E"), "peekVent");
        am.put("peekVent", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER ||
                    (ctx.currentPosition != PlayerPosition.FRONT_ROOM &&
                     ctx.currentPosition != PlayerPosition.PEEKING_VENT)) return;

                if (ctx.currentPosition != PlayerPosition.PEEKING_VENT) {
                    ctx.currentPosition = PlayerPosition.PEEKING_VENT;
                    ctx.logEvent("👀 Kamu mengecek ventilasi...");
                    if (ctx.enemyB.isAtDoor() && ctx.enemyB.getPatienceTimer() == 2) {
                        ctx.logEvent("👤 [SAFE] Kamu memergoki Hina di ventilasi! Dia mundur.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        combatSystem.startRetreatAnimation(ctx.enemyB);
                    }
                } else {
                    ctx.currentPosition = PlayerPosition.FRONT_ROOM;
                    ctx.logEvent("👀 Kamu berhenti mengecek ventilasi.");
                }
                ctx.officePanel.repaint();
            }
        });

        // F — Senter (Flashlight)
        im.put(KeyStroke.getKeyStroke("F"), "toggleFlashlight");
        am.put("toggleFlashlight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER || ctx.currentPosition != PlayerPosition.FRONT_ROOM) return;
                ctx.logEvent("🔦 Senter tidak bisa dimatikan (selalu menyala).");
                AudioManager.playSound("/assets/audio/sfx/button_click.wav");
                ctx.officePanel.repaint();
            }
        });

        // W — Masuk kabinet
        im.put(KeyStroke.getKeyStroke("W"), "hideCabinet");
        am.put("hideCabinet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER ||
                    ctx.currentPosition != PlayerPosition.BACK_ROOM ||
                    ctx.currentPosition == PlayerPosition.CABINET ||
                    ctx.lockpickPopupPanel.isVisible()) return;
                enterCabinet();
            }
        });

        // S — Multi-aksi kontekstual
        AbstractAction sAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER) return;

                if (ctx.lockpickPopupPanel.isVisible()) {
                    lockpickSystem.stopQte();
                    ctx.lockpickPopupPanel.setVisible(false);
                    updateUIVisibility();
                    ctx.logEvent("🔧 [INTERACT] Kamu mundur dari gembok.");
                } else if (ctx.currentPosition == PlayerPosition.CABINET &&
                           ctx.currentState != GameState.STRUGGLING) {
                    exitCabinet();
                } else if (ctx.currentPosition == PlayerPosition.BACK_ROOM &&
                           ctx.currentPosition != PlayerPosition.CABINET) {
                    ctx.logEvent("🔧 [INTERACT] Mendekat ke gembok untuk mencongkel...");
                    ctx.lockpickPopupPanel.setVisible(true);
                    lockpickSystem.startQte();
                    updateUIVisibility();
                }
            }
        };
        im.put(KeyStroke.getKeyStroke("S"), "sAction");
        am.put("sAction", sAction);

        // ESC — Universal back
        AbstractAction escAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ctx.currentState == GameState.GAMEOVER || ctx.currentState == GameState.STRUGGLING) return;

                if (ctx.currentPosition == PlayerPosition.PEEKING_KEYHOLE) {
                    ctx.currentPosition = PlayerPosition.FRONT_ROOM;
                    ctx.logEvent("👀 Kamu berhenti mengintip lubang kunci.");
                    if (ctx.enemyA.isAtDoor() && (ctx.enemyA.getPatienceTimer() == 2 || ctx.enemyA.getPatienceTimer() == 1)) {
                        ctx.logEvent("👤 [SAFE] Kamu berhenti mengintip. The Red One pergi.");
                        AudioManager.playSound("/assets/audio/sfx/enemy_fail.wav");
                        combatSystem.startRetreatAnimation(ctx.enemyA);
                    }
                    ctx.officePanel.repaint();
                } else if (ctx.currentPosition == PlayerPosition.PEEKING_VENT) {
                    ctx.currentPosition = PlayerPosition.FRONT_ROOM;
                    ctx.logEvent("👀 Kamu berhenti mengecek ventilasi.");
                    ctx.officePanel.repaint();
                } else if (ctx.lockpickPopupPanel.isVisible()) {
                    lockpickSystem.stopQte();
                    ctx.lockpickPopupPanel.setVisible(false);
                    updateUIVisibility();
                    ctx.logEvent("🔧 [INTERACT] Kamu mundur dari gembok.");
                } else if (ctx.currentPosition == PlayerPosition.CABINET) {
                    exitCabinet();
                } else if (ctx.currentPosition == PlayerPosition.BACK_ROOM) {
                    ctx.currentPosition = PlayerPosition.FRONT_ROOM;
                    updateUIVisibility();
                    ctx.officePanel.repaint();
                }
            }
        };
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "escAction");
        am.put("escAction", escAction);
    }

    // ============================================================
    // UI VISIBILITY
    // ============================================================

    void updateUIVisibility() {
        if (ctx.currentState == GameState.GAMEOVER) return;
        boolean isLockpickOpen = ctx.lockpickPopupPanel.isVisible();
        boolean isFront = (ctx.currentPosition == PlayerPosition.FRONT_ROOM ||
                           ctx.currentPosition == PlayerPosition.PEEKING_KEYHOLE ||
                           ctx.currentPosition == PlayerPosition.PEEKING_VENT);
        boolean isHallway = (ctx.currentState == GameState.HALLWAY);

        ctx.btnDoor.setVisible(false);
        ctx.btnLookLeft.setVisible(!isLockpickOpen &&
                ctx.currentPosition != PlayerPosition.CABINET && !isHallway &&
                ctx.currentPosition != PlayerPosition.PEEKING_KEYHOLE &&
                ctx.currentPosition != PlayerPosition.PEEKING_VENT);
        ctx.btnLookRight.setVisible(!isLockpickOpen &&
                ctx.currentPosition != PlayerPosition.CABINET && !isHallway &&
                ctx.currentPosition != PlayerPosition.PEEKING_KEYHOLE &&
                ctx.currentPosition != PlayerPosition.PEEKING_VENT);

        if (ctx.btnPeekKeyhole != null) {
            ctx.btnPeekKeyhole.setVisible(isFront && !isLockpickOpen && !isHallway &&
                    ctx.currentPosition != PlayerPosition.PEEKING_VENT);
            if (ctx.currentPosition == PlayerPosition.PEEKING_KEYHOLE) {
                ctx.btnPeekKeyhole.setText("BERHENTI MENGINTIP [Q]");
            } else {
                ctx.btnPeekKeyhole.setText("INTIP LUBANG KUNCI [Q]");
            }
        }

        if (ctx.btnPeekVent != null) {
            ctx.btnPeekVent.setVisible(isFront && !isLockpickOpen && !isHallway &&
                    ctx.currentPosition != PlayerPosition.PEEKING_KEYHOLE);
            if (ctx.currentPosition == PlayerPosition.PEEKING_VENT) {
                ctx.btnPeekVent.setText("BERHENTI MENGECEK [E]");
            } else {
                ctx.btnPeekVent.setText("INTIP VENTILASI [E]");
            }
        }
    }
}

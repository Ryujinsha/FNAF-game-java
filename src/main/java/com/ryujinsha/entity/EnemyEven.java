package com.ryujinsha.entity;

/**
 * EnemyEven (Hina) — Musuh Ventilasi dengan 4-Phase Flow:
 *   Phase 0: Vent Crawling (bergerak di dalam vent, belum terlihat)
 *   Phase 1: Show Up on Vent (sebagian terlihat di lubang ventilasi)
 *   Phase 2: Idle in Front (muncul penuh di depan pemain)
 *   Phase 3: Jumpscare trigger (patience habis)
 *
 * patienceTimer mapping:
 *   4 = Vent Crawling
 *   3 = Show Up on Vent (partial visibility)
 *   2 = Idle in Front (fully visible)
 *   1 = Final warning
 *   0 = Jumpscare
 */
public class EnemyEven extends Enemy {

    public EnemyEven(String name, int startAggression) {
        super(name, startAggression, 6);
        this.doorTarget = "VENT";
        this.jumpscarePath = "/assets/enemies/enemy_b_vent/jumpscare/hina_jumpscare.gif";
        this.quotePath = "/assets/audio/voice/quote_enemyEven.wav";
        this.patienceTimer = 4; // Start with 4 ticks for the new phase flow
    }

    @Override
    public void moveLogic() {
        if (rng.checkEvent(aggressionLevel)) {
            currentRoom -= 2;

            if (currentRoom <= 2) {
                isAtDoor = true;
                currentRoom = 0;
                patienceTimer = 4; // Reset to full 4-phase flow
                System.out.println("[ALERT] " + name + " (B) merangkak ke VENTILASI!");
            } else {
                System.out.println("[CCTV] " + name + " (B) berpindah ke Ruang " + currentRoom);
            }
        }
    }

    @Override
    public void retreat(int fallbackRoom) {
        isAtDoor = false;
        currentRoom = fallbackRoom;
        patienceTimer = 4; // Reset to 4 for the extended phase flow
        System.out.println("[ENEMY] " + name + " mundur ke ruang " + currentRoom);
    }

    /**
     * @return true jika Hina sedang dalam fase "Show Up on Vent" (Phase 1)
     */
    public boolean isShowingOnVent() {
        return isAtDoor && patienceTimer == 3;
    }

    /**
     * @return true jika Hina sedang dalam fase "Idle in Front" (Phase 2+)
     */
    public boolean isIdleInFront() {
        return isAtDoor && patienceTimer <= 2;
    }
}
package com.ryujinsha.entity;

public class EnemyOdd extends Enemy {

    public EnemyOdd(String name, int startAggression) {
        super(name, startAggression, 7);
        // The Red One menyerang melalui PINTU KIRI (front door)
        this.doorTarget = "LEFT";
        this.jumpscarePath = "/assets/enemies/enemy_a_door/jumpscare/jumpscare.gif";
        this.quotePath = "/assets/audio/voice/quote_enemyOdd.wav";
    }

    @Override
    public void moveLogic() {
        if (rng.checkEvent(aggressionLevel)) {
            currentRoom -= 2; // Lompat ke ruang ganjil berikutnya

            if (currentRoom <= 1) {
                isAtDoor = true;
                currentRoom = 0; // 0 = Pintu Depan
                System.out.println("[ALERT] " + name + " (A) muncul di PINTU DEPAN!");
            } else {
                System.out.println("[CCTV] " + name + " (A) berpindah ke Ruang " + currentRoom);
            }
        }
    }
}
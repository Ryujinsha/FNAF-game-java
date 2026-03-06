package com.ryujinsha.entity;

public class EnemyRandom extends Enemy {

    public EnemyRandom(String name, int startAggression) {
        super(name, startAggression, 7);
        this.doorTarget = "UNKNOWN"; 
        this.jumpscarePath = "/assets/enemies/jumpscare_c.png"; // ✨ BARU
        this.quotePath = "/assets/audio/voice/quote_enemyRandom.wav"; // ✨ BARU: Binding audio spesifik
    }

    @Override
    public void moveLogic() {
        if (rng.checkEvent(aggressionLevel)) {
            int jumpSteps = rng.roll() % 3 + 1; // Random maju 1 sampai 3 langkah
            currentRoom -= jumpSteps;
            
            if (currentRoom <= 1) {
                isAtDoor = true;
                currentRoom = 0;
                // Tentukan pintu serangan secara random
                this.doorTarget = (rng.roll() > 50) ? "LEFT" : "RIGHT";
                System.out.println("[ALERT] " + name + " (C) muncul tiba-tiba di PINTU " + doorTarget + "!");
            } else {
                System.out.println("[CCTV] Distorsi kamera... Sesuatu berpindah ke Ruang " + currentRoom);
            }
        }
    }
}
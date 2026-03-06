package com.ryujinsha.entity;

public class EnemyOdd extends Enemy {

public EnemyOdd(String name, int startAggression) {
        super(name, startAggression, 7); 
        this.doorTarget = "LEFT"; 
        this.jumpscarePath = "/assets/enemies/jumpscare_a.png"; // ✨ BARU
                this.quotePath = "/assets/audio/voice/quote_enemyOdd.wav"; // ✨ BARU: Binding audio spesifik
    }

    @Override
    public void moveLogic() {
        if (rng.checkEvent(aggressionLevel)) {
            currentRoom -= 2; // Lompat ke ruang ganjil berikutnya
            
            if (currentRoom <= 1) {
                isAtDoor = true;
                currentRoom = 0; // 0 = Pintu
                System.out.println("[ALERT] " + name + " (A) muncul di PINTU KIRI!");
            } else {
                System.out.println("[CCTV] " + name + " (A) berpindah ke Ruang " + currentRoom);
            }
        }
    }
}
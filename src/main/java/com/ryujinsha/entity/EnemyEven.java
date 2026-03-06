package com.ryujinsha.entity;

public class EnemyEven extends Enemy {

    public EnemyEven(String name, int startAggression) {
        super(name, startAggression, 6); 
        this.doorTarget = "RIGHT"; 
        this.jumpscarePath = "/assets/enemies/jumpscare_b.png"; 
        this.quotePath = "/assets/audio/voice/quote_enemyEven.wav"; // ✨ BARU: Binding audio spesifik
    }

    @Override
    public void moveLogic() {
        if (rng.checkEvent(aggressionLevel)) {
            currentRoom -= 2; // Lompat ke ruang genap berikutnya
            
            if (currentRoom <= 2) { // Ruang 2 adalah batas sebelum pintu
                isAtDoor = true;
                currentRoom = 0;
                System.out.println("[ALERT] " + name + " (B) muncul di PINTU KANAN!");
            } else {
                System.out.println("[CCTV] " + name + " (B) berpindah ke Ruang " + currentRoom);
            }
        }
    }
}
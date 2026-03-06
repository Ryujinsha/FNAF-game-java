package com.ryujinsha.entity;

import com.ryujinsha.system.RNGManager;

public abstract class Enemy extends Entity {
    protected int currentRoom;
    protected int aggressionLevel;
    protected RNGManager rng;
    
protected boolean isAtDoor;
    protected String doorTarget; 
    protected int patienceTimer; 
    protected String jumpscarePath;
    protected String quotePath; // ✨ BARU: Menyimpan path untuk quote khusus

    public Enemy(String name, int startAggression, int startRoom) {
        super(name);
        this.aggressionLevel = startAggression;
        this.currentRoom = startRoom;
        this.rng = new RNGManager();
        this.isAtDoor = false;
        this.patienceTimer = 3; 
    }

    // ✨ BARU: Getter untuk diambil oleh GameGUI nanti
    public String getJumpscarePath() {
        return jumpscarePath;
    }
    public String getQuotePath() { // ✨ BARU: Getter untuk audio
        return quotePath;
    }

    // Method abstract yang wajib diisi oleh masing-masing tipe hantu
    public abstract void moveLogic();

    @Override
    public void act() {
        if (!isAtDoor) {
            moveLogic();
        } else {
            countdownJumpscare();
        }
    }

    protected void countdownJumpscare() {
        patienceTimer--;
        System.out.println("[DANGER] " + name + " sedang bernapas di " + doorTarget + " DOOR. Waktu tersisa: " + patienceTimer);
        if (patienceTimer <= 0) {
            System.out.println("💀 JUMPSCARE! " + name + " menerkammu!");
            // GameEngine akan menangkap logic ini untuk Game Over
        }
    }

    public void retreat(int fallbackRoom) {
        isAtDoor = false;
        currentRoom = fallbackRoom;
        patienceTimer = 3; // Reset kesabaran
        System.out.println("[ENEMY] " + name + " mundur ke ruang " + currentRoom);
    }

    public boolean isAtDoor() { return isAtDoor; }
    public String getDoorTarget() { return doorTarget; }
    public int getCurrentRoom() { return currentRoom; }

    public int getPatienceTimer() {
        return patienceTimer;
    }
}
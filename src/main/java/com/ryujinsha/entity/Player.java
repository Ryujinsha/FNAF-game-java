package com.ryujinsha.entity;

import com.ryujinsha.system.PowerSystem;
import com.ryujinsha.system.SanitySystem;

public class Player extends Entity {
    private PowerSystem powerResource;
    private SanitySystem sanityResource; // ✨ Tambahan baru

    // State lingkungan pemain
    private boolean isTabletOpen;
    private boolean isLeftDoorClosed;
    private boolean isRightDoorClosed;
    private boolean isLeftLightOn;
    private boolean isRightLightOn;

    public Player(String name) {
        super(name);
        this.powerResource = new PowerSystem();
        this.sanityResource = new SanitySystem();
        
        // Semua mati/terbuka di awal
        this.isTabletOpen = false;
        this.isLeftDoorClosed = false;
        this.isRightDoorClosed = false;
        this.isLeftLightOn = false;
        this.isRightLightOn = false;
    }

    // ... (Getter Power dan Sanity) ...
    public PowerSystem getPower() { return powerResource; }
    public SanitySystem getSanity() { return sanityResource; }

    public void toggleTablet() {
        this.isTabletOpen = !this.isTabletOpen;
        System.out.println("[PLAYER] Tablet is now " + (isTabletOpen ? "OPEN" : "CLOSED"));
    }

    // Jika tablet terbuka atau pintu ditutup, panggil ini di Game Loop
    public void applyStress() {
        if (isTabletOpen || isLeftDoorClosed || isRightDoorClosed) {
            sanityResource.dropSanity(2); // Turun 2% per tick
            powerResource.consumePower(1); // Listrik juga tersedot
        }
        if (isLeftLightOn || isRightLightOn) {
            powerResource.consumePower(2); 
        }
    }

    // Method untuk pintu kiri
    public void toggleLeftDoor() {
        this.isLeftDoorClosed = !this.isLeftDoorClosed;
        System.out.println("[PLAYER] Left Door is now " + (isLeftDoorClosed ? "CLOSED" : "OPEN"));
    }
    // 1. Tambahkan dua method ini di bawah method toggleDoor
    public void toggleLeftLight() {
        this.isLeftLightOn = !this.isLeftLightOn;
        System.out.println("[PLAYER] Left Light is " + (isLeftLightOn ? "ON" : "OFF"));
    }

    public void toggleRightLight() {
        this.isRightLightOn = !this.isRightLightOn;
        System.out.println("[PLAYER] Right Light is " + (isRightLightOn ? "ON" : "OFF"));
    }


    // Method untuk pintu kanan
    public void toggleRightDoor() {
        this.isRightDoorClosed = !this.isRightDoorClosed;
        System.out.println("[PLAYER] Right Door is now " + (isRightDoorClosed ? "CLOSED" : "OPEN"));
    }

    @Override
    public void act() {
        // Interaksi pemain diproses di GameEngine nanti
    }

    // Getters untuk state pintu/tablet (Dibutuhkan GameEngine untuk cek Jumpscare)
    public boolean isTabletOpen() { return isTabletOpen; }
    public boolean isLeftDoorClosed() { return isLeftDoorClosed; }
    public boolean isRightDoorClosed() { return isRightDoorClosed; }
    public boolean isLeftLightOn() { return isLeftLightOn; }
    public boolean isRightLightOn() { return isRightLightOn; }
}
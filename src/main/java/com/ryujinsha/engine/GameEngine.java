package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AudioManager;
import com.ryujinsha.system.TimeSystem;
import java.util.Scanner;

public class GameEngine {
    private Player player;
    private EnemyOdd enemyA;
    private EnemyEven enemyB;
    private EnemyRandom enemyC;
    private TimeSystem timeSystem;
    
    private boolean isRunning;
    private boolean areEnemiesActive; // Ter-trigger saat tablet pertama kali dibuka
    private int tickCounter; // Penghitung untuk pergantian jam

    public GameEngine() {
        this.player = new Player("Night Guard");
        this.enemyA = new EnemyOdd("Diddy", 20); // Aggression 20%
        this.enemyB = new EnemyEven("Epstein", 20);
        this.enemyC = new EnemyRandom("Bowo", 15);
        this.timeSystem = new TimeSystem();
        
        this.isRunning = true;
        this.areEnemiesActive = false;
        this.tickCounter = 0;
    }

    public void start() {
        System.out.println("=========================================");
        System.out.println("  NIGHT SHIFT SURVIVAL - CONSOLE ALPHA   ");
        System.out.println("=========================================");
        System.out.println("Tugasmu: Bertahan hingga 06:00 AM.");
        System.out.println("Hati-hati, membuka Tablet memancing MEREKA.");
        System.out.println("=========================================\n");
        
        gameLoop();
    }

    private void gameLoop() {
        Scanner scanner = new Scanner(System.in);

        while (isRunning) {
            // STEP 1: Tampilkan UI Console
            renderStatus();

            // STEP 2: Input Pemain
            System.out.println("\n[AKSI]: 1. Toggle Tablet | 2. Toggle Left Door | 3. Toggle Right Door | 4. Wait");
            System.out.print("> ");
            String input = scanner.nextLine();
            processPlayerAction(input);

            // STEP 3: Proses Stress & Listrik Player
            player.applyStress();

            // STEP 4: Pergerakan Musuh (Jika Tablet sudah pernah dibuka)
            if (areEnemiesActive) {
                enemyA.act();
                enemyB.act();
                enemyC.act();
            }

            // STEP 5: Cek Kondisi Menang/Kalah/Jumpscare
            checkWinLossConditions();

            // STEP 6: Waktu Berjalan
            tickCounter++;
            if (tickCounter >= 10) { // Setiap 10 aksi = 1 Jam
                timeSystem.advanceTime();
                tickCounter = 0;
                System.out.println("\n🔔 TENG TONG... Waktu menunjukkan " + timeSystem.getFormattedTime() + " 🔔\n");
            }
        }
        scanner.close();
    }

    private void renderStatus() {
        System.out.println("\n--- " + timeSystem.getFormattedTime() + " ---");
        System.out.println("Power : " + player.getPower().getCurrentPower() + "% | Sanity: " + player.getSanity().getSanityLevel() + "%");
        System.out.println("Tablet: " + (player.isTabletOpen() ? "[ON]" : "[OFF]"));
        System.out.println("L-Door: " + (player.isLeftDoorClosed() ? "[CLOSED]" : "[OPEN]") + 
                         " | R-Door: " + (player.isRightDoorClosed() ? "[CLOSED]" : "[OPEN]"));
    }

    private void processPlayerAction(String input) {
        // Jika listrik mati, alat tidak bisa dipakai
        if (player.getPower().isPowerEmpty()) {
            System.out.println("[SYSTEM FAILURE] Listrik habis! Tidak bisa melakukan apa-apa...");
            return;
        }

        switch (input) {
            case "1":
                player.toggleTablet();
                if (player.isTabletOpen() && !areEnemiesActive) {
                    areEnemiesActive = true;
                    System.out.println("⚠️ [WARNING] Sesuatu menyadari kehadiranmu...");
                }
                break;
            case "2":
                player.toggleLeftDoor();
                break;
            case "3":
                player.toggleRightDoor();
                break;
            case "4":
                System.out.println("[PLAYER] Menahan napas dan menunggu...");
                break;
            default:
                System.out.println("[ERROR] Input tidak valid.");
        }
    }

    private void checkWinLossConditions() {
        // 1. Cek Menang
        if (timeSystem.isMorning()) {
            System.out.println("\n☀️ 06:00 AM ☀️");
            System.out.println("Kamu selamat malam ini...");
            isRunning = false;
            return;
        }

        // 2. Cek Sanity
        if (player.getSanity().isInsane()) {
            System.out.println("\n🧠 KEWARASAN HABIS...");
            System.out.println("Kamu berhalusinasi dan berlari keluar ruangan... menembus kegelapan.");
            System.out.println("GAME OVER");
            isRunning = false;
            return;
        }

        // 3. Cek Jumpscare vs Pintu
        checkDoorDefense(enemyA);
        checkDoorDefense(enemyB);
        checkDoorDefense(enemyC);
    }

    // Logic krusial untuk mengecek apakah hantu tertahan pintu atau berhasil Jumpscare
    // Logic krusial untuk mengecek apakah hantu tertahan pintu atau berhasil Jumpscare
    private void checkDoorDefense(Enemy enemy) {
        // Mencegah musuh lain ikut jumpscare jika game sudah over di frame ini
        if (!isRunning) return; 

        if (enemy.isAtDoor()) {
            boolean isDefended = false;
            
            if (enemy.getDoorTarget().equals("LEFT") && player.isLeftDoorClosed()) {
                isDefended = true;
            } else if (enemy.getDoorTarget().equals("RIGHT") && player.isRightDoorClosed()) {
                isDefended = true;
            }

            if (isDefended) {
                System.out.println("💥 *BAM BAM BAM* Sesuatu memukul pintu " + enemy.getDoorTarget() + "!");
                
                // ✨ AUDIO TRIGER: Gedoran Pintu
                // Anda bisa menggunakan math random di sini nanti untuk variasi 1-3
                AudioManager.playSound("/assets/audio/sfx/door_bang_1.wav"); 
                
                enemy.retreat(7); 
            } else if (enemy.getPatienceTimer() <= 0) { 
                System.out.println("\n💀 JUMPSCARE! " + enemy.getName() + " masuk dari pintu " + enemy.getDoorTarget() + "!");
                
                // ✨ AUDIO TRIGER: Jumpscare & Game Over Sequence
                triggerGameOverSequence(enemy);
            }
        }
    }

    // ✨ BARU: Metode terpisah untuk mengelola alur timing Game Over
    private void triggerGameOverSequence(Enemy killer) {
        isRunning = false; // Hentikan loop game utama
        AudioManager.stopAllSounds(); 
        AudioManager.playSound("/assets/audio/sfx/jumpscare_scream.wav");

        // Karena ini berbasis Console, kita gunakan Thread.sleep untuk menahan penutupan program
        // agar sekuens audio memiliki waktu untuk dimainkan.
        try {
            Thread.sleep(2000); // Jeda 2 detik (durasi suara jumpscare)
            
            System.out.println("... (Hening) ...");
            if (killer.getQuotePath() != null) {
                AudioManager.playSound(killer.getQuotePath());
            }
            
            Thread.sleep(4000); // Tunggu 4 detik agar musuh selesai berbicara
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("=========================================");
        System.out.println("               GAME OVER                 ");
        System.out.println("=========================================");
    }
}
package com.ryujinsha.engine;

import com.ryujinsha.entity.*;
import com.ryujinsha.system.AssetCache;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ✨ GameContext — Container untuk seluruh state game.
 *
 * Digunakan oleh semua subsystem (InputHandler, CombatSystem, dll.)
 * sebagai single source of truth untuk state permainan.
 */
public class GameContext {

    // ============================================================
    // CORE REFERENCES
    // ============================================================
    MainFrame mainFrame;
    JPanel officePanel;
    JLayeredPane layeredPane;

    // ============================================================
    // ENTITIES
    // ============================================================
    Player player;
    EnemyOdd enemyA;
    EnemyEven enemyB;
    boolean areEnemiesActive = true;

    // ============================================================
    // STATE
    // ============================================================
    GameState currentState = GameState.PLAYING;
    PlayerPosition currentPosition = PlayerPosition.FRONT_ROOM;

    // ============================================================
    // UI COMPONENTS
    // ============================================================
    JLabel statusLabel;
    PixelButton btnDoor, btnLookLeft, btnLookRight, btnPeekKeyhole, btnPeekVent;
    JPanel lockpickPopupPanel;
    JPanel endScreenPanel;
    JLabel endTitleLabel, endMessageLabel;

    // ============================================================
    // LOCKPICK STATE
    // ============================================================
    int lockBars = 0;
    double qteIndicatorPos = 0.0;
    int qteDirection = 1;
    boolean qteActive = false;
    JPanel qteRenderPanel;
    Timer qteAnimTimer;
    Timer lockDrainTimer;

    // ============================================================
    // STRUGGLE STATE
    // ============================================================
    int struggleValue = 50;
    Timer struggleTimer;
    Enemy currentAttacker = null;
    boolean hidEarly = false;
    Image qteBodyImg, qteHandLeftImg, qteHandRightImg;
    int struggleAnimCounter = 0;

    // ============================================================
    // RETREAT STATE
    // ============================================================
    boolean isRetreating = false;
    int retreatAnimTicks = 0;
    Enemy lastDefeatedEnemy = null;
    Image retreatImg = null;
    Timer retreatTimer;

    // ============================================================
    // VISUAL STATE
    // ============================================================
    Image doorEnemyVisual = null;
    Image ventEnemyVisual = null;
    float vignetteIntensity = 0f;
    boolean isFlickering = false;
    float flickerAlpha = 0f;
    Timer flickerTimer;
    boolean hasFlickeredForPhase2 = false;
    boolean hasFlickeredForEnemyA = false;
    boolean hasFlickeredForDanger = false;

    // ============================================================
    // TIMERS
    // ============================================================
    Timer gameLoopTimer;
    Timer quoteTimer;

    // ============================================================
    // LEADERBOARD & TIMER TRACKING
    // ============================================================
    long gameStartTimeMs = 0;
    long gameEndTimeMs = 0;
    String currentGameMode = "STORY"; // "STORY" atau "ENDLESS"
    int endlessScore = 0;

    // ============================================================
    // FLASHLIGHT & INCOMING
    // ============================================================
    boolean isFlashlightOn = true;
    boolean incomingDialogVisible = false;
    Timer incomingTimer;

    // ============================================================
    // HALLWAY
    // ============================================================
    boolean hasHallwayKey = false;
    boolean hallwayCutsceneActive = false;
    int hallwayCutsceneIndex = 0;
    String[] hallwayCutsceneTexts = {
        "Kamu merasa sendirian. Namun, tidak ada jalan kembali",
        "Kamu berlari sampai kau menuju sebuah ruangan kecil. Kamu mendengar suara serangga dan kamu berasumsi kamu akan bebas",
        "Namun, tidak semudah itu. Mereka marah dan ingin menyerangmu"
    };
    String currentDisplayedText = "";
    Timer typingTimer;
    int typingCharIndex = 0;

    // ============================================================
    // DEV MODE
    // ============================================================
    List<String> devLogs = new ArrayList<>();

    // ============================================================
    // ASSET PATHS
    // ============================================================
    static final String PATH_FRONT_ROOM = "/assets/rooms/front_room.png";
    static final String PATH_FRONT_DOOR = "/assets/rooms/front_door.png";
    static final String PATH_BACK_DOOR = "/assets/rooms/back_door.png";
    static final String PATH_BACK_DOOR_OPENED = "/assets/rooms/back_door_opened.png";
    static final String PATH_LOCK_DOOR = "/assets/rooms/lock_door.png";
    static final String PATH_HALLWAY = "/assets/rooms/hallway.png";

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    GameContext(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initGameData();
        preloadCriticalAssets();
    }

    // ============================================================
    // INISIALISASI DATA
    // ============================================================

    void initGameData() {
        this.player = new Player("Night Guard");
        this.enemyA = new EnemyOdd("The Red One", 20);
        this.enemyB = new EnemyEven("Hina", 20);

        this.areEnemiesActive = false;
        this.currentState = GameState.HALLWAY;
        this.currentPosition = PlayerPosition.FRONT_ROOM;
        this.incomingDialogVisible = true;
        this.hasHallwayKey = false;
        this.hallwayCutsceneActive = false;
        this.hallwayCutsceneIndex = 0;
        this.lockBars = 0;
        this.qteIndicatorPos = 0.0;
        this.qteDirection = 1;
        this.qteActive = false;
        this.struggleValue = 50;
        this.doorEnemyVisual = null;
        this.ventEnemyVisual = null;
        this.hidEarly = false;
        this.struggleAnimCounter = 0;
        this.isFlickering = false;
        this.flickerAlpha = 0f;
        this.hasFlickeredForPhase2 = false;
        this.hasFlickeredForEnemyA = false;
        this.hasFlickeredForDanger = false;
        this.isRetreating = false;
        this.retreatAnimTicks = 0;
        this.lastDefeatedEnemy = null;
        this.retreatImg = null;

        // Leaderboard tracking
        this.gameStartTimeMs = 0;
        this.gameEndTimeMs = 0;
        this.endlessScore = 0;
    }

    private void preloadCriticalAssets() {
        AssetCache.preload(
                PATH_FRONT_ROOM, PATH_FRONT_DOOR,
                PATH_BACK_DOOR, PATH_BACK_DOOR_OPENED,
                PATH_LOCK_DOOR, PATH_HALLWAY,
                "/assets/keyhole/tunnel.png",
                "/assets/keyhole/tunnel_door.png",
                "/assets/vent/vent_back.png",
                "/assets/vent/vent_front.png",
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-1.png",
                "/assets/enemies/enemy_a_door/idle/the-red-idle-phase-2.png",
                "/assets/enemies/enemy_b_vent/idle/hina_idle_phase_1.png",
                "/assets/enemies/enemy_b_vent/idle/hina_idle_phase-2.png");
    }

    // ============================================================
    // UTILITY
    // ============================================================

    Enemy getEnemyAtDoor() {
        if (enemyA.isAtDoor()) return enemyA;
        if (enemyB.isAtDoor()) return enemyB;
        return null;
    }

    void logEvent(String message) {
        System.out.println(message);
        if (MainFrame.isDevMode) {
            devLogs.add(0, message);
            if (devLogs.size() > 10)
                devLogs.remove(devLogs.size() - 1);
            if (officePanel != null)
                officePanel.repaint();
        }
    }

    // ============================================================
    // GETTERS FOR RENDERER
    // ============================================================
    public GameState getCurrentState() { return currentState; }
    public PlayerPosition getCurrentPosition() { return currentPosition; }
    public float getVignetteIntensity() { return vignetteIntensity; }
    public boolean isFlickering() { return isFlickering; }
    public float getFlickerAlpha() { return flickerAlpha; }
    public boolean isRetreating() { return isRetreating; }
    public EnemyOdd getEnemyA() { return enemyA; }
    public EnemyEven getEnemyB() { return enemyB; }
    public Image getDoorEnemyVisual() { return doorEnemyVisual; }
    public Player getPlayer() { return player; }
    public int getLockBars() { return lockBars; }
    public Image getRetreatImg() { return retreatImg; }
    public Enemy getLastDefeatedEnemy() { return lastDefeatedEnemy; }
    public int getRetreatAnimTicks() { return retreatAnimTicks; }
    public Image getQteBodyImg() { return qteBodyImg; }
    public Image getQteHandLeftImg() { return qteHandLeftImg; }
    public Image getQteHandRightImg() { return qteHandRightImg; }
    public int getStruggleValue() { return struggleValue; }
    public List<String> getDevLogs() { return devLogs; }
    public boolean isIncomingDialogVisible() { return incomingDialogVisible; }
    public boolean isHallwayCutsceneActive() { return hallwayCutsceneActive; }
    public int getHallwayCutsceneIndex() { return hallwayCutsceneIndex; }
    public String[] getHallwayCutsceneTexts() { return hallwayCutsceneTexts; }
    public boolean hasHallwayKey() { return hasHallwayKey; }
    public String getCurrentDisplayedText() { return currentDisplayedText; }
    public boolean isPeekingKeyhole() { return currentPosition == PlayerPosition.PEEKING_KEYHOLE; }
    public boolean isPeekingVent() { return currentPosition == PlayerPosition.PEEKING_VENT; }
    public boolean isFlashlightOn() { return isFlashlightOn; }

    // ============================================================
    // LEADERBOARD GETTERS
    // ============================================================

    /** Hitung waktu bermain dalam milidetik sejak game dimulai */
    public long getElapsedTimeMs() {
        if (gameStartTimeMs == 0) return 0;
        if (gameEndTimeMs > 0) return gameEndTimeMs - gameStartTimeMs;
        return System.currentTimeMillis() - gameStartTimeMs;
    }

    /** Format waktu bermain ke MM:SS */
    public String getFormattedElapsedTime() {
        long totalMs = getElapsedTimeMs();
        long minutes = totalMs / 60000;
        long seconds = (totalMs % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String getCurrentGameMode() { return currentGameMode; }
    public int getEndlessScore() { return endlessScore; }
}

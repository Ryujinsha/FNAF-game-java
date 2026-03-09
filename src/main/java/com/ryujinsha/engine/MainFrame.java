package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel containerPanel;

    public MainFrame() {
        setTitle("Asdos : Sudah Malam atau Sudah Tahu?");
        setSize(1300, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false); // Mencegah window diubah ukurannya agar UI tidak berantakan

        cardLayout = new CardLayout();
        containerPanel = new JPanel(cardLayout);

        // Tambahkan Menu Utama sebagai layar pertama saat aplikasi dibuka
        containerPanel.add(new MainMenuGUI(this), "MENU");
        
        add(containerPanel);
    }

    // Metode ajaib untuk memindah layar
    public void showScreen(String screenName) {
        // Kita membuat instance baru setiap kali layar dipanggil agar state (animasi/data) selalu ter-reset
        if (screenName.equals("CUTSCENE")) {
            containerPanel.add(new CutsceneGUI(this), "CUTSCENE");
        } else if (screenName.equals("GAME")) {
            GameGUI game = new GameGUI(this);
            containerPanel.add(game, "GAME");
            game.startGame(); // Memulai timer game saat layar siap
        } else if (screenName.equals("MENU")) {
            containerPanel.add(new MainMenuGUI(this), "MENU");
        }
        
        cardLayout.show(containerPanel, screenName);
        containerPanel.revalidate();
        containerPanel.repaint();
    }
}
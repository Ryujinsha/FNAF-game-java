package com.ryujinsha;

import com.ryujinsha.engine.MainMenuGUI; // Import kelas Main Menu
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Standard praktik Java Swing agar UI berjalan di thread yang aman
        SwingUtilities.invokeLater(() -> {
            // Memanggil Main Menu sebagai layar pertama, bukan langsung GameGUI
            MainMenuGUI menu = new MainMenuGUI();
            menu.setVisible(true);
        });
    }
}
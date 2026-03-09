package com.ryujinsha.engine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CutsceneGUI extends JPanel { // ✨ REFACTOR 1: Berubah menjadi JPanel
    private MainFrame mainFrame; // ✨ REFACTOR 2: Menyimpan referensi MainFrame

    private JTextArea textArea;
    private String fullText;
    private int charIndex = 0;
    private Timer typewriterTimer;
    private boolean isFinished = false;
    private JLabel hintLabel;

    public CutsceneGUI(MainFrame mainFrame) { // ✨ REFACTOR 3: Menerima parameter MainFrame
        this.mainFrame = mainFrame;
        
        // Hapus setTitle, setSize, dll. karena ukuran diatur oleh MainFrame
        setBackground(Color.BLACK); // Mengganti getContentPane().setBackground()
        setLayout(new BorderLayout());

        // Narasi Intro
        fullText = "Tahun 2026...\n\n" +
                   "Mereka bilang ini hanya pekerjaan mudah.\n" +
                   "Menjaga tempat ini dari tengah malam hingga jam 6 pagi.\n" +
                   "Hanya mengawasi kamera dan memastikan pintu terkunci.\n\n" +
                   "Tapi belakangan ini, barang-barang berpindah dengan sendirinya...\n" +
                   "Dan aku bersumpah, bayangan di lorong itu baru saja bergerak.\n\n" +
                   "Shift malamku dimulai sekarang.";

        // Menggunakan JTextArea agar teks bisa multi-baris secara rapi
        textArea = new JTextArea();
        textArea.setBackground(Color.BLACK);
        textArea.setForeground(Color.WHITE);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 32));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setMargin(new Insets(150, 150, 150, 150)); 
        add(textArea, BorderLayout.CENTER);

        hintLabel = new JLabel("Klik untuk melewati...", SwingConstants.CENTER);
        hintLabel.setForeground(Color.DARK_GRAY);
        hintLabel.setFont(new Font("Consolas", Font.PLAIN, 18));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        add(hintLabel, BorderLayout.SOUTH);

        // Timer untuk efek ketikan (muncul setiap 50 milidetik)
        typewriterTimer = new Timer(50, e -> processTypewriterEffect());
        
        // Listener untuk mendeteksi klik mouse di seluruh layar
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleScreenClick();
            }
        };
        
        // Memasang listener ke komponen agar klik di mana saja terdeteksi
        addMouseListener(clickListener);
        textArea.addMouseListener(clickListener);
        
        // Mulai animasi
        typewriterTimer.start();
    }

    private void processTypewriterEffect() {
        if (charIndex < fullText.length()) {
            textArea.append(String.valueOf(fullText.charAt(charIndex)));
            
            // Opsional: Anda bisa memutar SFX ketikan kecil di sini
            // com.ryujinsha.system.AudioManager.playSound("/assets/audio/sfx/typewriter_tick.wav");
            
            charIndex++;
        } else {
            typewriterTimer.stop();
            isFinished = true;
            hintLabel.setText("Klik untuk memulai shift...");
            hintLabel.setForeground(Color.WHITE); // Terangkan teks saat selesai
        }
    }

    private void handleScreenClick() {
        if (!isFinished) {
            // Klik pertama: Skip animasi, tampilkan seluruh teks secara instan
            typewriterTimer.stop();
            textArea.setText(fullText);
            isFinished = true;
            hintLabel.setText("Klik untuk memulai shift...");
            hintLabel.setForeground(Color.WHITE);
        } else {
            // ✨ REFACTOR 4: Klik kedua memerintahkan MainFrame pindah ke layar GAME
            // Hapus dispose() dan instansiasi JFrame baru karena MainFrame yang bertugas memindah kartu
            mainFrame.showScreen("GAME");
        }
    }
}
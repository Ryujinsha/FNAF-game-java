package com.ryujinsha.system;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ImageLoader {
    
    // Memuat gambar sebagai ImageIcon (untuk JLabel)
    public static ImageIcon loadIcon(String path) {
        URL imgUrl = ImageLoader.class.getResource(path);
        if (imgUrl != null) {
            return new ImageIcon(imgUrl);
        } else {
            System.err.println("Could not find image: " + path);
            return null;
        }
    }

    // Memuat gambar dan mengubah ukurannya (Scaled)
    public static ImageIcon loadScaledIcon(String path, int width, int height) {
        URL imgUrl = ImageLoader.class.getResource(path);
        if (imgUrl != null) {
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImg);
        } else {
            System.err.println("Could not find image: " + path);
            return null;
        }
    }
}










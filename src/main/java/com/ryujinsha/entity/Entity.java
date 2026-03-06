package com.ryujinsha.entity;

public abstract class Entity {
    protected String name;
    protected boolean isAlive;

    // Constructor
    public Entity(String name) {
        this.name = name;
        this.isAlive = true;
    }

    // Getter & Setter dasar
    public String getName() {
        return name;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    // Method abstract yang wajib di-override oleh class turunannya
    public abstract void act(); 
}
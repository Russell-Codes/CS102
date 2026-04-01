package com.g1t7.splendor.model;

import java.io.Serializable;

/**
 * Represents a playable development card in the game.
 * Acts as a pure Data Transfer Object (POJO).
 */
public class Card implements Serializable {

    private int tier;
    private GemColor gemColor;
    private int value;

    // Index mapping: 0=WHITE, 1=BLUE, 2=GREEN, 3=RED, 4=BLACK, 5=GOLD (Gold is
    // always 0 cost)
    private int[] cost = new int[Player.TOTAL_COIN_TYPES];
    private boolean reserved = false;

    public Card() {
    }

    public Card(int tier, GemColor gemColor, int value, int[] cost) {
        this.tier = tier;
        this.gemColor = gemColor;
        this.value = value;
        this.cost = cost;
    }

    // --- GETTERS AND SETTERS ---

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public GemColor getGemColor() {
        return gemColor;
    }

    public void setGemColor(GemColor gemColor) {
        this.gemColor = gemColor;
    }

    /**
     * * Alias kept specifically for Thymeleaf template convenience.
     * 
     * @return The gem color bonus this card provides.
     */
    public GemColor getColor() {
        return gemColor;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int[] getCost() {
        return cost;
    }

    public void setCost(int[] cost) {
        this.cost = cost;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }
}
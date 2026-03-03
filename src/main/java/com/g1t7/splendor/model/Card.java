package com.g1t7.splendor.model;

import java.io.Serializable;

public class Card implements Serializable {


    private int tier;
    private GemColor gemColor;
    private int value;
    // cost[i] = amount of GemColor.fromIndex(i) gems needed; cost[5] (gold) always 0
    private int[] cost = new int[6];
    private boolean reserved = false;
    private String imagePath;

    public Card() {}

    public Card(int tier, GemColor gemColor, int value, int[] cost) {
        this.tier = tier;
        this.gemColor = gemColor;
        this.value = value;
        this.cost = cost;
    }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public GemColor getGemColor() { return gemColor; }
    public void setGemColor(GemColor gemColor) { this.gemColor = gemColor; }

    /** Alias kept for Thymeleaf convenience */
    public GemColor getColor() { return gemColor; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public int[] getCost() { return cost; }
    public void setCost(int[] cost) { this.cost = cost; }

    public boolean isReserved() { return reserved; }
    public void setReserved(boolean reserved) { this.reserved = reserved; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}

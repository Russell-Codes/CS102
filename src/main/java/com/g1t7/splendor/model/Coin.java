package com.g1t7.splendor.model;

import java.io.Serializable;

public class Coin implements Serializable {

    private GemColor gemColor;
    private boolean selected;

    public Coin() {
    }

    public Coin(GemColor gemColor) {
        this.gemColor = gemColor;
    }

    /** Alias for getGemColor() kept for compatibility */
    public GemColor getColor() {
        return gemColor;
    }

    public GemColor getGemColor() {
        return gemColor;
    }

    public void setGemColor(GemColor gemColor) {
        this.gemColor = gemColor;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

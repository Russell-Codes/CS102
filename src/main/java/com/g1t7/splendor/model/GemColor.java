package com.g1t7.splendor.model;

public enum GemColor {
    WHITE, BLUE, GREEN, RED, BLACK, GOLD;

    public static GemColor fromIndex(int i) {
        return values()[i];
    }
}
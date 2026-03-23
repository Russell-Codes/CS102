package com.g1t7.splendor.model;

import java.io.Serializable;

public class GemColor implements Serializable {

    public static final GemColor WHITE = new GemColor("WHITE", 0);
    public static final GemColor BLUE = new GemColor("BLUE", 1);
    public static final GemColor GREEN = new GemColor("GREEN", 2);
    public static final GemColor RED = new GemColor("RED", 3);
    public static final GemColor BLACK = new GemColor("BLACK", 4);
    public static final GemColor GOLD = new GemColor("GOLD", 5);

    private static final GemColor[] VALUES = { WHITE, BLUE, GREEN, RED, BLACK, GOLD };

    private final String colorName;
    private final int index;

    private GemColor(String colorName, int index) {
        this.colorName = colorName;
        this.index = index;
    }

    public static GemColor fromIndex(int i) {
        return VALUES[i];
    }

    public static GemColor valueOf(String name) {
        for (GemColor gc : VALUES) {
            if (gc.colorName.equalsIgnoreCase(name))
                return gc;
        }
        throw new IllegalArgumentException("Unknown GemColor: " + name);
    }

    public static GemColor[] values() {
        return VALUES.clone();
    }

    /** Returns the color name in ALL_CAPS (mirrors enum.name()). */
    public String name() {
        return colorName;
    }

    /** Returns the positional index 0–5 (mirrors enum.ordinal()). */
    public int ordinal() {
        return index;
    }

    @Override
    public String toString() {
        return colorName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GemColor))
            return false;
        return index == ((GemColor) o).index;
    }

    @Override
    public int hashCode() {
        return index;
    }

    /** Ensures deserialization returns the canonical singleton. */
    protected Object readResolve() {
        return VALUES[index];
    }
}

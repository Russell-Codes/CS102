package com.g1t7.splendor.model;

/**
 * Shared utility for CSS class mapping.
 */
public class Common {

    private Common() {}

    /** Returns the CSS class name for a given GemColor index. */
    public static String getCssClass(GemColor color) {
        return "gem-" + color.name().toLowerCase();
    }

    /** Returns the CSS class name for a given GemColor ordinal index. */
    public static String getCssClass(int index) {
        return getCssClass(GemColor.fromIndex(index));
    }
}

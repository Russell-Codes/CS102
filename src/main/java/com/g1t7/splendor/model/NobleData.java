package com.g1t7.splendor.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Static noble tile definitions.
 * Noble(white, blue, green, red, black) — each requiring that many purchased
 * cards of the matching gem color.
 */
public class NobleData {

    private NobleData() {}

    public static List<Noble> buildNobles() {
        List<Noble> nobles = new ArrayList<>();
        // 5 nobles; Game will shuffle and keep 3
        nobles.add(new Noble(4, 4, 0, 0, 0)); // 4 white, 4 blue
        nobles.add(new Noble(3, 0, 0, 3, 3)); // 3 white, 3 red, 3 black
        nobles.add(new Noble(0, 4, 4, 0, 0)); // 4 blue, 4 green
        nobles.add(new Noble(0, 0, 3, 3, 3)); // 3 green, 3 red, 3 black
        nobles.add(new Noble(3, 3, 3, 0, 0)); // 3 white, 3 blue, 3 green
        return nobles;
    }
}

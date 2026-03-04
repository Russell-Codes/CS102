package com.g1t7.splendor.model;

import java.util.ArrayList;
import java.util.List;

/**
<<<<<<< HEAD
 * Standard 10 noble tile definitions for Splendor.
 * Noble(white, blue, green, red, black) — each requiring that many purchased
 * cards of the matching gem colour.  All nobles grant 3 prestige points.
=======
 * Static noble tile definitions.
 * Noble(white, blue, green, red, black) — each requiring that many purchased
 * cards of the matching gem color.
>>>>>>> remotes/origin/init
 */
public class NobleData {

    private NobleData() {}

    public static List<Noble> buildNobles() {
        List<Noble> nobles = new ArrayList<>();
<<<<<<< HEAD
        // Standard 10 noble tiles from the Splendor rulebook
        nobles.add(new Noble(4, 4, 0, 0, 0)); // Mary Stuart
        nobles.add(new Noble(0, 4, 4, 0, 0)); // Suleiman the Magnificent
        nobles.add(new Noble(0, 0, 4, 4, 0)); // Catherine de Medici
        nobles.add(new Noble(0, 0, 0, 4, 4)); // Charles V
        nobles.add(new Noble(4, 0, 0, 0, 4)); // Machiavelli
        nobles.add(new Noble(3, 3, 3, 0, 0)); // Isabella I of Castile
        nobles.add(new Noble(0, 3, 3, 3, 0)); // Francis I of France
        nobles.add(new Noble(0, 0, 3, 3, 3)); // Anne of Brittany
        nobles.add(new Noble(3, 0, 0, 3, 3)); // Elizabeth of Austria
        nobles.add(new Noble(3, 3, 0, 0, 3)); // Henry VIII
=======
        // 5 nobles; Game will shuffle and keep 3
        nobles.add(new Noble(4, 4, 0, 0, 0)); // 4 white, 4 blue
        nobles.add(new Noble(3, 0, 0, 3, 3)); // 3 white, 3 red, 3 black
        nobles.add(new Noble(0, 4, 4, 0, 0)); // 4 blue, 4 green
        nobles.add(new Noble(0, 0, 3, 3, 3)); // 3 green, 3 red, 3 black
        nobles.add(new Noble(3, 3, 3, 0, 0)); // 3 white, 3 blue, 3 green
>>>>>>> remotes/origin/init
        return nobles;
    }
}

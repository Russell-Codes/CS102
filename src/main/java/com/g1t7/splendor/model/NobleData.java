package com.g1t7.splendor.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/**
 * Loads Noble tiles from an external CSV file specified in config.properties.
 * Falls back to a standard hardcoded list if the CSV cannot be read.
 */
public class NobleData {

    private NobleData() {
    }

    public static List<Noble> buildNobles(String csvPath) {
        List<Noble> nobles = new ArrayList<>();

        try (InputStream is = new ClassPathResource(csvPath).getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                if (line.isEmpty())
                    continue;

                // Use -1 to keep empty strings (since Points column is empty in nobles.csv)
                String[] parts = line.split(",", -1);
                if (parts.length < 8)
                    continue;

                try {
                    // parts: ID, Name, Points, White, Blue, Green, Red, Black
                    int white = Integer.parseInt(parts[3].trim());
                    int blue = Integer.parseInt(parts[4].trim());
                    int green = Integer.parseInt(parts[5].trim());
                    int red = Integer.parseInt(parts[6].trim());
                    int black = Integer.parseInt(parts[7].trim());

                    nobles.add(new Noble(white, blue, green, red, black));
                } catch (Exception e) {
                    System.err.println("[NobleData] Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("[NobleData] CSV not found at classpath:" + csvPath + " - using fallback nobles");
            return buildFallbackNobles();
        }

        if (nobles.isEmpty()) {
            return buildFallbackNobles();
        }

        System.out.println("[NobleData] Loaded " + nobles.size() + " nobles from " + csvPath);
        return nobles;
    }

    public static List<Noble> buildNobles() {
        return buildNobles(GameConfig.getNobleFile());
    }

    private static List<Noble> buildFallbackNobles() {
        List<Noble> nobles = new ArrayList<>();
        nobles.add(new Noble(4, 4, 0, 0, 0));
        nobles.add(new Noble(0, 4, 4, 0, 0));
        nobles.add(new Noble(0, 0, 4, 4, 0));
        nobles.add(new Noble(0, 0, 0, 4, 4));
        nobles.add(new Noble(4, 0, 0, 0, 4));
        nobles.add(new Noble(3, 3, 3, 0, 0));
        nobles.add(new Noble(0, 3, 3, 3, 0));
        nobles.add(new Noble(0, 0, 3, 3, 3));
        nobles.add(new Noble(3, 0, 0, 3, 3));
        nobles.add(new Noble(3, 3, 0, 0, 3));
        return nobles;
    }
}
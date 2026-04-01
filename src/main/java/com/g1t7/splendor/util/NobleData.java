package com.g1t7.splendor.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.g1t7.splendor.model.Noble;

public class NobleData {

    private static final Logger logger = LoggerFactory.getLogger(NobleData.class);
    private static final int EXPECTED_CSV_COLUMNS = 8;

    private NobleData() {
    }

    public static List<Noble> buildNobles(String csvPath) {
        List<Noble> nobles = new ArrayList<>();
        int idCounter = 1; // Add an ID counter

        try (InputStream is = new ClassPathResource(csvPath).getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split(",", -1);
                if (parts.length < EXPECTED_CSV_COLUMNS)
                    continue;

                try {
                    int white = Integer.parseInt(parts[3].trim());
                    int blue = Integer.parseInt(parts[4].trim());
                    int green = Integer.parseInt(parts[5].trim());
                    int red = Integer.parseInt(parts[6].trim());
                    int black = Integer.parseInt(parts[7].trim());

                    nobles.add(new Noble(idCounter++, white, blue, green, red, black));
                } catch (Exception e) {
                    logger.warn("Skipping malformed line in Nobles CSV: {}", line);
                }
            }
        } catch (IOException e) {
            logger.error("CSV not found at classpath: {} - using fallback nobles", csvPath);
            return buildFallbackNobles();
        }

        if (nobles.isEmpty()) {
            return buildFallbackNobles();
        }

        logger.info("Loaded {} nobles from {}", nobles.size(), csvPath);
        return nobles;
    }

    private static List<Noble> buildFallbackNobles() {
        List<Noble> nobles = new ArrayList<>();
        // Update fallback nobles with static IDs 1 through 10
        nobles.add(new Noble(1, 4, 4, 0, 0, 0));
        nobles.add(new Noble(2, 0, 4, 4, 0, 0));
        nobles.add(new Noble(3, 0, 0, 4, 4, 0));
        nobles.add(new Noble(4, 0, 0, 0, 4, 4));
        nobles.add(new Noble(5, 4, 0, 0, 0, 4));
        nobles.add(new Noble(6, 3, 3, 3, 0, 0));
        nobles.add(new Noble(7, 0, 3, 3, 3, 0));
        nobles.add(new Noble(8, 0, 0, 3, 3, 3));
        nobles.add(new Noble(9, 3, 0, 0, 3, 3));
        nobles.add(new Noble(10, 3, 3, 0, 0, 3));
        return nobles;
    }
}
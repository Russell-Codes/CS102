package com.g1t7.splendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Splendor Spring Boot app.
 */
@SpringBootApplication
@EnableScheduling
public class SplendorApplication {

    /**
     * Starts Spring Boot.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SplendorApplication.class, args);
    }

}

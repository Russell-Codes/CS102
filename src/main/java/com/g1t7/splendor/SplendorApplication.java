package com.g1t7.splendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The monolithic execution root for the Splendor Spring Boot Application.
 * Bootstraps the embedded Apache Tomcat Servlet container, coordinates the 
 * internal component scan (finding Controllers and Services), and loads application contexts.
 */
@SpringBootApplication
@EnableScheduling
public class SplendorApplication {

    /**
     * Standard Java execution entry point. 
     * Passes execution context over to SpringApplication's lifecycle engine.
     *
     * @param args Raw command-line arguments optionally injected during server startup.
     */
	public static void main(String[] args) {
		SpringApplication.run(SplendorApplication.class, args);
	}

}

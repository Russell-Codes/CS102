package com.g1t7.splendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SplendorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SplendorApplication.class, args);
	}

}

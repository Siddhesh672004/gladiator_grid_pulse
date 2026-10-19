package com.gridpulse.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// core service (port 8082): accounts, tracks, sessions, bookings, race control and feedback
// scheduling runs the live-board heartbeat
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class CoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreApplication.class, args);
	}

}

package com.college.events;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class EventsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventsBackendApplication.class, args);
    }
}

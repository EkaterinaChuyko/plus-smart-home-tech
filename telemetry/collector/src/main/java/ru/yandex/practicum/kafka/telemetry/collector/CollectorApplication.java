package ru.yandex.practicum.kafka.telemetry.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CollectorApplication {
    public static void main(String[] args) {
        System.out.println("=== COLLECTOR STARTING ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        try {
            ConfigurableApplicationContext context = SpringApplication.run(CollectorApplication.class, args);
            System.out.println("=== COLLECTOR STARTED SUCCESSFULLY ===");

            System.out.println("Active profiles: " + String.join(", ", context.getEnvironment().getActiveProfiles()));
        } catch (Exception e) {
            System.out.println("=== COLLECTOR FAILED TO START ===");
            e.printStackTrace();
            throw e;
        }
    }
}
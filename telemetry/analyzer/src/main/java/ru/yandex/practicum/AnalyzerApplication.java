package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.processor.HubEventProcessor;
import ru.yandex.practicum.processor.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApplication {

    public static void main(String[] args) {
        System.out.println("=== AnalyzerApplication main() ===");
        System.out.println("Current directory: " + System.getProperty("user.dir"));
        System.out.println("Java version: " + System.getProperty("java.version"));

        try {
            ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApplication.class, args);
            System.out.println("=== Spring context loaded ===");
            System.out.println("Beans count: " + context.getBeanDefinitionCount());

            String[] beanNames = context.getBeanDefinitionNames();
            System.out.println("First 10 beans:");
            for (int i = 0; i < Math.min(10, beanNames.length); i++) {
                System.out.println("  - " + beanNames[i]);
            }

            System.out.println("Getting HubEventProcessor bean...");
            HubEventProcessor hubEventProcessor = context.getBean(HubEventProcessor.class);
            System.out.println("HubEventProcessor: " + hubEventProcessor);

            System.out.println("Getting SnapshotProcessor bean...");
            SnapshotProcessor snapshotProcessor = context.getBean(SnapshotProcessor.class);
            System.out.println("SnapshotProcessor: " + snapshotProcessor);

            System.out.println("Starting HubEventProcessor thread...");
            Thread hubEventsThread = new Thread(hubEventProcessor);
            hubEventsThread.setName("HubEventHandlerThread");
            hubEventsThread.start();
            System.out.println("HubEventProcessor thread started");

            System.out.println("Starting SnapshotProcessor...");
            snapshotProcessor.start();

        } catch (Exception e) {
            System.err.println("=== AnalyzerApplication failed ===");
            e.printStackTrace();
            throw e;
        }
    }
}
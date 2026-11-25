package com.riftradar;

import com.riftradar.config.RiftRadarProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RiftRadarProperties.class)
public class RiftradarApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiftradarApplication.class, args);
    }
}

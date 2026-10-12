package com.ag.deprem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DepremApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepremApplication.class, args);
    }

}

package com.codeyzer.mine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MineyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MineyzerApplication.class, args);
    }
} 
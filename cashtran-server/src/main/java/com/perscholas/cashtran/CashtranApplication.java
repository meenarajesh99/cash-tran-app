package com.perscholas.cashtran;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class CashtranApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashtranApplication.class, args);
    }
}


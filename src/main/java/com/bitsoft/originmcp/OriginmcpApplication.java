package com.bitsoft.originmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OriginmcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(OriginmcpApplication.class, args);
    }
}

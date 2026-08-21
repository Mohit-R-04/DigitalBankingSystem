package com.banking.interbankservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class InterbankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterbankServiceApplication.class, args);
    }
}

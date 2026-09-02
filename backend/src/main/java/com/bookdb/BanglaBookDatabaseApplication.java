package com.bookdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BanglaBookDatabaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BanglaBookDatabaseApplication.class, args);
    }
}

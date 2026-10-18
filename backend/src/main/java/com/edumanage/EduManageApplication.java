package com.edumanage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduManageApplication.class, args);
    }
}

package com.example.qmx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.example.qmx.mapper")
public class QmxApplication {

    public static void main(String[] args) {
        SpringApplication.run(QmxApplication.class, args);
    }

}

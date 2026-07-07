package com.seatwise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.seatwise.mapper")
public class SeatwiseApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeatwiseApplication.class, args);
    }
}

package com.icinfo.taskmanagement;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.icinfo.taskmanagement.mapper")
@SpringBootApplication
public class TaskManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
    }
}

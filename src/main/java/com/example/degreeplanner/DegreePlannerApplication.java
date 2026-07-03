package com.example.degreeplanner;

import com.example.degreeplanner.engine.SchedulePlanner;
import com.example.degreeplanner.repository.CourseRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DegreePlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DegreePlannerApplication.class, args);
    }

    @Bean
    SchedulePlanner schedulePlanner(CourseRepository courseRepository) {
        return new SchedulePlanner(courseRepository);
    }
}

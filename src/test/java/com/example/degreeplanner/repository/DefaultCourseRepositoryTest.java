package com.example.degreeplanner.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DefaultCourseRepositoryTest {
    @Autowired
    CourseRepository courseRepository;

    @Test
    void defaultProfileUsesMockCourseRepository() {
        assertThat(courseRepository).isInstanceOf(MockCourseRepository.class);
        assertThat(courseRepository.findAll()).hasSize(40);
    }
}

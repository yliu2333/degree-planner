package com.example.degreeplanner.repository;

import com.example.degreeplanner.domain.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    List<Course> findAll();

    Optional<Course> findById(String id);
}

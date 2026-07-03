package com.example.degreeplanner.engine;

public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(String courseId) {
        super("Course not found: " + courseId);
    }
}

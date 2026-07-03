package com.example.degreeplanner.domain;

import java.util.List;

public record Semester(
        Term term,
        int year,
        List<Course> courses,
        int totalCredits
) {
}

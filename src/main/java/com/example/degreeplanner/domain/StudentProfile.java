package com.example.degreeplanner.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record StudentProfile(
        @NotNull Set<String> completedCourseIds,
        @NotNull Set<String> targetCourseIds,
        @Min(1) int maxCreditsPerSemester,
        @NotNull Term startTerm
) {
}

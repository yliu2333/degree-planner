package com.example.degreeplanner.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record Course(
        @NotBlank String id,
        @NotBlank String name,
        @Min(1) int credits,
        @NotEmpty Set<Term> offeredTerms,
        @NotNull List<String> prerequisiteIds
) {
}

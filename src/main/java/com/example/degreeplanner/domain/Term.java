package com.example.degreeplanner.domain;

public enum Term {
    FALL,
    SPRING;

    public Term next() {
        return this == FALL ? SPRING : FALL;
    }

    public boolean startsNewPlanYearAfterThisTerm() {
        return this == SPRING;
    }
}

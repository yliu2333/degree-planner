package com.example.degreeplanner.engine;

public class UnplannableScheduleException extends RuntimeException {
    public UnplannableScheduleException(String message) {
        super(message);
    }
}

package com.example.degreeplanner.api;

import com.example.degreeplanner.engine.CourseNotFoundException;
import com.example.degreeplanner.engine.PrerequisiteCycleException;
import com.example.degreeplanner.engine.UnplannableScheduleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PrerequisiteCycleException.class)
    public ResponseEntity<ApiError> handleCycle(PrerequisiteCycleException ex) {
        return ResponseEntity.badRequest().body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler({
            CourseNotFoundException.class,
            UnplannableScheduleException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }
}

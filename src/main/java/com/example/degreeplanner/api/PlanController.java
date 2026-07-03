package com.example.degreeplanner.api;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.SemesterPlan;
import com.example.degreeplanner.domain.StudentProfile;
import com.example.degreeplanner.engine.SchedulePlanner;
import com.example.degreeplanner.repository.CourseRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlanController {
    private final CourseRepository courseRepository;
    private final SchedulePlanner schedulePlanner;

    public PlanController(CourseRepository courseRepository, SchedulePlanner schedulePlanner) {
        this.courseRepository = courseRepository;
        this.schedulePlanner = schedulePlanner;
    }

    @GetMapping("/courses")
    public List<Course> courses() {
        return courseRepository.findAll();
    }

    @PostMapping("/plan")
    public SemesterPlan plan(@Valid @RequestBody StudentProfile profile) {
        return schedulePlanner.generatePlan(profile);
    }
}

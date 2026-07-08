package com.example.degreeplanner.repository;

import com.example.degreeplanner.domain.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("!postgres")
public class MockCourseRepository implements CourseRepository {
    private final List<Course> courses;
    private final Map<String, Course> coursesById;

    public MockCourseRepository(ObjectMapper objectMapper) {
        try (InputStream inputStream = new ClassPathResource("courses.json").getInputStream()) {
            List<Course> loadedCourses = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            this.courses = List.copyOf(loadedCourses);
            this.coursesById = indexById(loadedCourses);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load mock course catalog from courses.json", ex);
        }
    }

    @Override
    public List<Course> findAll() {
        return courses;
    }

    @Override
    public Optional<Course> findById(String id) {
        return Optional.ofNullable(coursesById.get(id));
    }

    private Map<String, Course> indexById(List<Course> loadedCourses) {
        Map<String, Course> index = new LinkedHashMap<>();
        for (Course course : loadedCourses) {
            Course previous = index.put(course.id(), course);
            if (previous != null) {
                throw new IllegalStateException("Duplicate course id in mock catalog: " + course.id());
            }
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(index));
    }
}

package com.example.degreeplanner.repository;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Term;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@Profile("postgres")
public class PostgresCourseRepository implements CourseRepository {
    private final JdbcTemplate jdbcTemplate;

    public PostgresCourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Course> findAll() {
        Map<String, CourseRow> coursesById = new LinkedHashMap<>();
        jdbcTemplate.query("""
                        SELECT id, name, credits, offered_terms
                        FROM courses
                        ORDER BY id
                        """,
                (RowCallbackHandler) rs -> coursesById.put(rs.getString("id"), courseRow(rs)));

        if (coursesById.isEmpty()) {
            return List.of();
        }

        for (PrerequisiteRow prerequisite : findAllPrerequisites()) {
            CourseRow course = coursesById.get(prerequisite.courseId());
            if (course != null) {
                course.prerequisiteIds().add(prerequisite.prerequisiteId());
            }
        }

        return coursesById.values()
                .stream()
                .map(CourseRow::toCourse)
                .toList();
    }

    @Override
    public Optional<Course> findById(String id) {
        List<CourseRow> rows = jdbcTemplate.query("""
                        SELECT id, name, credits, offered_terms
                        FROM courses
                        WHERE id = ?
                        """,
                (rs, rowNum) -> courseRow(rs),
                id);

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        CourseRow course = rows.get(0);
        findPrerequisitesByCourseId(id).forEach(prerequisite -> course.prerequisiteIds().add(prerequisite.prerequisiteId()));
        return Optional.of(course.toCourse());
    }

    private CourseRow courseRow(ResultSet rs) throws SQLException {
        return new CourseRow(
                rs.getString("id"),
                rs.getString("name"),
                rs.getInt("credits"),
                parseTerms(rs.getString("offered_terms")),
                new LinkedHashSet<>()
        );
    }

    private List<PrerequisiteRow> findAllPrerequisites() {
        return jdbcTemplate.query("""
                        SELECT course_id, prerequisite_id
                        FROM prerequisites
                        ORDER BY course_id, prerequisite_id
                        """,
                (rs, rowNum) -> new PrerequisiteRow(rs.getString("course_id"), rs.getString("prerequisite_id")));
    }

    private List<PrerequisiteRow> findPrerequisitesByCourseId(String courseId) {
        return jdbcTemplate.query("""
                        SELECT course_id, prerequisite_id
                        FROM prerequisites
                        WHERE course_id = ?
                        ORDER BY prerequisite_id
                        """,
                (rs, rowNum) -> new PrerequisiteRow(rs.getString("course_id"), rs.getString("prerequisite_id")),
                courseId);
    }

    private Set<Term> parseTerms(String offeredTerms) {
        Set<Term> terms = new LinkedHashSet<>();
        Arrays.stream(offeredTerms.split(","))
                .map(String::trim)
                .filter(term -> !term.isEmpty())
                .map(Term::valueOf)
                .forEach(terms::add);
        return Collections.unmodifiableSet(terms);
    }

    private record CourseRow(
            String id,
            String name,
            int credits,
            Set<Term> offeredTerms,
            Set<String> prerequisiteIds
    ) {
        Course toCourse() {
            return new Course(id, name, credits, offeredTerms, List.copyOf(prerequisiteIds));
        }
    }

    private record PrerequisiteRow(String courseId, String prerequisiteId) {
    }
}

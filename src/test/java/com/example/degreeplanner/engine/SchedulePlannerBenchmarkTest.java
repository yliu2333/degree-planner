package com.example.degreeplanner.engine;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.SemesterPlan;
import com.example.degreeplanner.domain.StudentProfile;
import com.example.degreeplanner.domain.Term;
import com.example.degreeplanner.repository.CourseRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulePlannerBenchmarkTest {
    private static final int LAYERS = 20;

    @Test
    void generatePlanForTenThousandCourseLayeredDagCompletesUnderTwoSeconds() {
        BenchmarkCatalog catalog = layeredCatalog(10_000, LAYERS);
        SchedulePlanner planner = new SchedulePlanner(new StubCourseRepository(catalog.courses()));
        StudentProfile profile = new StudentProfile(
                Set.of(),
                catalog.finalLayerCourseIds(),
                catalog.coursesPerLayer(),
                Term.FALL
        );

        long startNanos = System.nanoTime();
        SemesterPlan plan = planner.generatePlan(profile);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        System.out.printf("SchedulePlanner 10,000-course benchmark: %.3f ms%n", elapsed.toNanos() / 1_000_000.0);

        assertThat(plan.semesters()).hasSize(LAYERS);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    @Disabled("Manual benchmark for larger catalogs; run explicitly when profiling.")
    @Test
    void generatePlanForOneHundredThousandCourseLayeredDagCompletesUnderTwoSeconds() {
        BenchmarkCatalog catalog = layeredCatalog(100_000, LAYERS);
        SchedulePlanner planner = new SchedulePlanner(new StubCourseRepository(catalog.courses()));
        StudentProfile profile = new StudentProfile(
                Set.of(),
                catalog.finalLayerCourseIds(),
                catalog.coursesPerLayer(),
                Term.FALL
        );

        long startNanos = System.nanoTime();
        SemesterPlan plan = planner.generatePlan(profile);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        System.out.printf("SchedulePlanner 100,000-course benchmark: %.3f ms%n", elapsed.toNanos() / 1_000_000.0);

        assertThat(plan.semesters()).hasSize(LAYERS);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    private static BenchmarkCatalog layeredCatalog(int courseCount, int layers) {
        if (courseCount % layers != 0) {
            throw new IllegalArgumentException("courseCount must divide evenly by layers");
        }

        int coursesPerLayer = courseCount / layers;
        List<Course> courses = new ArrayList<>(courseCount);
        Set<String> finalLayerCourseIds = new LinkedHashSet<>();

        for (int layer = 0; layer < layers; layer++) {
            for (int offset = 0; offset < coursesPerLayer; offset++) {
                String courseId = courseId(layer, offset);
                List<String> prerequisiteIds = layer == 0
                        ? List.of()
                        : prerequisitesFor(layer, offset, coursesPerLayer);

                courses.add(new Course(
                        courseId,
                        "Synthetic Course " + courseId,
                        1,
                        Set.of(Term.FALL, Term.SPRING),
                        prerequisiteIds
                ));

                if (layer == layers - 1) {
                    finalLayerCourseIds.add(courseId);
                }
            }
        }

        return new BenchmarkCatalog(List.copyOf(courses), Set.copyOf(finalLayerCourseIds), coursesPerLayer);
    }

    private static List<String> prerequisitesFor(int layer, int offset, int coursesPerLayer) {
        List<String> prerequisiteIds = new ArrayList<>(3);
        int prerequisiteCount = 1 + Math.floorMod(layer * 31 + offset, 3);

        for (int i = 0; i < prerequisiteCount; i++) {
            int prerequisiteOffset = Math.floorMod(offset * 17 + layer * 13 + i * 37, coursesPerLayer);
            prerequisiteIds.add(courseId(layer - 1, prerequisiteOffset));
        }

        return List.copyOf(prerequisiteIds);
    }

    private static String courseId(int layer, int offset) {
        return "L%02d-C%05d".formatted(layer, offset);
    }

    private record BenchmarkCatalog(List<Course> courses, Set<String> finalLayerCourseIds, int coursesPerLayer) {
    }

    private static class StubCourseRepository implements CourseRepository {
        private final List<Course> courses;
        private final Map<String, Course> coursesById;

        private StubCourseRepository(List<Course> courses) {
            this.courses = courses;
            this.coursesById = new LinkedHashMap<>();
            for (Course course : courses) {
                coursesById.put(course.id(), course);
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
    }
}

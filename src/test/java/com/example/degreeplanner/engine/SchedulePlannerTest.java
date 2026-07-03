package com.example.degreeplanner.engine;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Semester;
import com.example.degreeplanner.domain.SemesterPlan;
import com.example.degreeplanner.domain.StudentProfile;
import com.example.degreeplanner.domain.Term;
import com.example.degreeplanner.repository.CourseRepository;
import com.example.degreeplanner.repository.MockCourseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulePlannerTest {

    @Test
    void topologicalOrderPlacesPrerequisitesBeforeDependents() {
        SchedulePlanner planner = new SchedulePlanner(repository(List.of(
                course("A", 3, Set.of(Term.FALL, Term.SPRING)),
                course("B", 3, Set.of(Term.FALL, Term.SPRING), "A"),
                course("C", 3, Set.of(Term.FALL, Term.SPRING), "B"),
                course("D", 3, Set.of(Term.FALL, Term.SPRING), "A", "C")
        )));

        List<String> order = planner.topologicalOrderForTargets(Set.of("D"))
                .stream()
                .map(Course::id)
                .toList();

        assertThat(order).containsExactly("A", "B", "C", "D");
    }

    @Test
    void cycleDetectionIncludesCyclePath() {
        SchedulePlanner planner = new SchedulePlanner(repository(List.of(
                course("A", 3, Set.of(Term.FALL, Term.SPRING), "B"),
                course("B", 3, Set.of(Term.FALL, Term.SPRING), "C"),
                course("C", 3, Set.of(Term.FALL, Term.SPRING), "A")
        )));

        assertThatThrownBy(() -> planner.topologicalOrderForTargets(Set.of("A")))
                .isInstanceOf(PrerequisiteCycleException.class)
                .hasMessageContaining("A -> B -> C -> A")
                .extracting(ex -> ((PrerequisiteCycleException) ex).getCyclePath())
                .isEqualTo(List.of("A", "B", "C", "A"));
    }

    @Test
    void greedyPlannerRespectsCreditLimitBoundary() {
        SchedulePlanner planner = new SchedulePlanner(repository(List.of(
                course("A", 3, Set.of(Term.FALL, Term.SPRING)),
                course("B", 3, Set.of(Term.FALL, Term.SPRING)),
                course("C", 2, Set.of(Term.FALL, Term.SPRING))
        )));

        SemesterPlan plan = planner.generatePlan(new StudentProfile(
                Set.of(),
                Set.of("A", "B", "C"),
                5,
                Term.FALL
        ));

        assertThat(plan.semesters()).hasSize(2);
        assertThat(plan.semesters()).allSatisfy(semester -> assertThat(semester.totalCredits()).isLessThanOrEqualTo(5));
        assertThat(plan.semesters().get(0).courses()).extracting(Course::id).containsExactly("A", "C");
        assertThat(plan.semesters().get(0).totalCredits()).isEqualTo(5);
    }

    @Test
    void plannerSkipsTermsWhenCourseIsNotOffered() {
        SchedulePlanner planner = new SchedulePlanner(repository(List.of(
                course("A", 3, Set.of(Term.FALL)),
                course("B", 3, Set.of(Term.SPRING), "A")
        )));

        SemesterPlan plan = planner.generatePlan(new StudentProfile(
                Set.of(),
                Set.of("B"),
                6,
                Term.SPRING
        ));

        assertThat(plan.semesters()).extracting(Semester::term).containsExactly(Term.FALL, Term.SPRING);
        assertThat(plan.semesters().get(0).courses()).extracting(Course::id).containsExactly("A");
        assertThat(plan.semesters().get(1).courses()).extracting(Course::id).containsExactly("B");
    }

    @Test
    void fullPlanForFreshmanProfileReachesSeniorProjectInPrerequisiteOrder() {
        MockCourseRepository repository = new MockCourseRepository(new ObjectMapper());
        SchedulePlanner planner = new SchedulePlanner(repository);

        SemesterPlan plan = planner.generatePlan(new StudentProfile(
                Set.of(),
                Set.of("CS491"),
                16,
                Term.FALL
        ));

        List<String> scheduledIds = plan.semesters()
                .stream()
                .flatMap(semester -> semester.courses().stream())
                .map(Course::id)
                .toList();

        assertThat(repository.findAll()).hasSize(40);
        assertThat(scheduledIds).contains("CS101", "CS102", "CS110", "CS201", "CS250", "CS301", "CS490", "CS491");
        assertThat(scheduledIds.get(scheduledIds.size() - 1)).isEqualTo("CS491");
        assertThat(plan.semesters()).allSatisfy(semester -> assertThat(semester.totalCredits()).isLessThanOrEqualTo(16));
        assertPrerequisitesCompletedEarlier(plan);
    }

    private void assertPrerequisitesCompletedEarlier(SemesterPlan plan) {
        Map<String, Integer> semesterByCourse = new HashMap<>();
        List<Course> allScheduledCourses = new ArrayList<>();

        for (int i = 0; i < plan.semesters().size(); i++) {
            for (Course course : plan.semesters().get(i).courses()) {
                semesterByCourse.put(course.id(), i);
                allScheduledCourses.add(course);
            }
        }

        for (Course course : allScheduledCourses) {
            for (String prerequisiteId : course.prerequisiteIds()) {
                assertThat(semesterByCourse.get(prerequisiteId))
                        .as(course.id() + " requires " + prerequisiteId + " in an earlier semester")
                        .isLessThan(semesterByCourse.get(course.id()));
            }
        }
    }

    private static Course course(String id, int credits, Set<Term> offeredTerms, String... prerequisiteIds) {
        return new Course(id, "Course " + id, credits, offeredTerms, List.of(prerequisiteIds));
    }

    private static CourseRepository repository(List<Course> courses) {
        Map<String, Course> index = new LinkedHashMap<>();
        for (Course course : courses) {
            index.put(course.id(), course);
        }
        return new CourseRepository() {
            @Override
            public List<Course> findAll() {
                return courses;
            }

            @Override
            public Optional<Course> findById(String id) {
                return Optional.ofNullable(index.get(id));
            }
        };
    }
}

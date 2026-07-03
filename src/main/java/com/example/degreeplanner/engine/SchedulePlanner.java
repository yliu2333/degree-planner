package com.example.degreeplanner.engine;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Semester;
import com.example.degreeplanner.domain.SemesterPlan;
import com.example.degreeplanner.domain.StudentProfile;
import com.example.degreeplanner.domain.Term;
import com.example.degreeplanner.repository.CourseRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class SchedulePlanner {
    private final CourseRepository courseRepository;

    public SchedulePlanner(CourseRepository courseRepository) {
        this.courseRepository = Objects.requireNonNull(courseRepository, "courseRepository is required");
    }

    public SemesterPlan generatePlan(StudentProfile profile) {
        validateProfile(profile);

        Map<String, Course> catalog = loadCatalogById();
        Map<String, Integer> catalogOrder = catalogOrder(catalog);

        // expand targets into their full prerequisite closure
        LinkedHashSet<String> requiredCourseIds = collectRequiredCourseIds(profile.targetCourseIds(), catalog);

        // Kahn's algorithm gives prerequisite-first order
        List<String> topologicalOrder = topologicalSort(requiredCourseIds, catalog, catalogOrder);

        // Completed courses satisfy prerequisites but do not need to consume seats or credits again.
        LinkedHashSet<String> unscheduledCourseIds = new LinkedHashSet<>(topologicalOrder);
        unscheduledCourseIds.removeAll(profile.completedCourseIds());

        validateCreditLimit(unscheduledCourseIds, catalog, profile.maxCreditsPerSemester());

        Set<String> completed = new HashSet<>(profile.completedCourseIds());
        List<Semester> semesters = new ArrayList<>();
        Term term = profile.startTerm();
        int year = 1;
        int consecutiveNoProgressTerms = 0;

        while (!unscheduledCourseIds.isEmpty()) {
            // A semester must only unlock future semesters, so we snapshot completed work before
            // selecting courses; this avoids placing a course and its prerequisite side by side.
            Set<String> completedBeforeSemester = Set.copyOf(completed);
            List<Course> selectedCourses = new ArrayList<>();
            int credits = 0;

            for (String courseId : topologicalOrder) {
                if (!unscheduledCourseIds.contains(courseId)) {
                    continue;
                }

                Course course = catalog.get(courseId);
                boolean offeredThisTerm = course.offeredTerms().contains(term);
                boolean prerequisitesDoneEarlier = completedBeforeSemester.containsAll(course.prerequisiteIds());
                boolean fitsCreditLimit = credits + course.credits() <= profile.maxCreditsPerSemester();

                if (offeredThisTerm && prerequisitesDoneEarlier && fitsCreditLimit) {
                    selectedCourses.add(course);
                    credits += course.credits();
                }
            }

            if (selectedCourses.isEmpty()) {
                consecutiveNoProgressTerms++;
                if (consecutiveNoProgressTerms >= 2) {
                    throw new UnplannableScheduleException(
                            "No schedulable courses found across both terms; check offerings and prerequisites.");
                }
            } else {
                consecutiveNoProgressTerms = 0;
                for (Course course : selectedCourses) {
                    unscheduledCourseIds.remove(course.id());
                    completed.add(course.id());
                }
                semesters.add(new Semester(term, year, List.copyOf(selectedCourses), credits));
            }

            if (term.startsNewPlanYearAfterThisTerm()) {
                year++;
            }
            term = term.next();
        }

        return new SemesterPlan(List.copyOf(semesters));
    }

    public List<Course> topologicalOrderForTargets(Set<String> targetCourseIds) {
        Map<String, Course> catalog = loadCatalogById();
        Map<String, Integer> catalogOrder = catalogOrder(catalog);
        LinkedHashSet<String> requiredCourseIds = collectRequiredCourseIds(targetCourseIds, catalog);
        return topologicalSort(requiredCourseIds, catalog, catalogOrder)
                .stream()
                .map(catalog::get)
                .toList();
    }

    private void validateProfile(StudentProfile profile) {
        Objects.requireNonNull(profile, "profile is required");
        Objects.requireNonNull(profile.completedCourseIds(), "completedCourseIds is required");
        Objects.requireNonNull(profile.targetCourseIds(), "targetCourseIds is required");
        Objects.requireNonNull(profile.startTerm(), "startTerm is required");
        if (profile.maxCreditsPerSemester() < 1) {
            throw new IllegalArgumentException("maxCreditsPerSemester must be at least 1");
        }
    }

    private Map<String, Course> loadCatalogById() {
        Map<String, Course> catalog = new LinkedHashMap<>();
        for (Course course : courseRepository.findAll()) {
            catalog.put(course.id(), course);
        }
        return catalog;
    }

    private Map<String, Integer> catalogOrder(Map<String, Course> catalog) {
        Map<String, Integer> order = new HashMap<>();
        int index = 0;
        for (String courseId : catalog.keySet()) {
            order.put(courseId, index++);
        }
        return order;
    }

    private LinkedHashSet<String> collectRequiredCourseIds(Set<String> targetCourseIds, Map<String, Course> catalog) {
        LinkedHashSet<String> required = new LinkedHashSet<>();
        for (String targetCourseId : targetCourseIds) {
            collectRequiredCourseIds(targetCourseId, catalog, required);
        }
        return required;
    }

    private void collectRequiredCourseIds(String courseId, Map<String, Course> catalog, Set<String> required) {
        Course course = catalog.get(courseId);
        if (course == null) {
            throw new CourseNotFoundException(courseId);
        }
        if (!required.add(courseId)) {
            return;
        }
        for (String prerequisiteId : course.prerequisiteIds()) {
            collectRequiredCourseIds(prerequisiteId, catalog, required);
        }
    }

    private List<String> topologicalSort(Set<String> requiredCourseIds,
                                         Map<String, Course> catalog,
                                         Map<String, Integer> catalogOrder) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> outgoingEdges = new HashMap<>();
        for (String courseId : requiredCourseIds) {
            inDegree.put(courseId, 0);
            outgoingEdges.put(courseId, new ArrayList<>());
        }

        for (String courseId : requiredCourseIds) {
            Course course = catalog.get(courseId);
            for (String prerequisiteId : course.prerequisiteIds()) {
                if (!requiredCourseIds.contains(prerequisiteId)) {
                    continue;
                }
                outgoingEdges.get(prerequisiteId).add(courseId);
                inDegree.put(courseId, inDegree.get(courseId) + 1);
            }
        }

        Comparator<String> byCatalogOrder = Comparator.comparingInt(id -> catalogOrder.getOrDefault(id, Integer.MAX_VALUE));
        Queue<String> ready = new PriorityQueue<>(byCatalogOrder);
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<String> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String courseId = ready.remove();
            ordered.add(courseId);

            outgoingEdges.get(courseId).stream()
                    .sorted(byCatalogOrder)
                    .forEach(dependentId -> {
                        int updatedInDegree = inDegree.get(dependentId) - 1;
                        inDegree.put(dependentId, updatedInDegree);
                        if (updatedInDegree == 0) {
                            ready.add(dependentId);
                        }
                    });
        }

        if (ordered.size() != requiredCourseIds.size()) {
            Set<String> unresolved = new LinkedHashSet<>(requiredCourseIds);
            unresolved.removeAll(ordered);
            throw new PrerequisiteCycleException(findCyclePath(unresolved, catalog));
        }

        return ordered;
    }

    private List<String> findCyclePath(Set<String> unresolvedCourseIds, Map<String, Course> catalog) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> stack = new ArrayDeque<>();

        for (String courseId : unresolvedCourseIds) {
            List<String> cycle = findCyclePath(courseId, unresolvedCourseIds, catalog, visiting, visited, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return List.copyOf(unresolvedCourseIds);
    }

    private List<String> findCyclePath(String courseId,
                                       Set<String> unresolvedCourseIds,
                                       Map<String, Course> catalog,
                                       Set<String> visiting,
                                       Set<String> visited,
                                       ArrayDeque<String> stack) {
        if (visited.contains(courseId)) {
            return List.of();
        }
        if (visiting.contains(courseId)) {
            return cycleFromStack(courseId, stack);
        }

        visiting.add(courseId);
        stack.addLast(courseId);

        for (String prerequisiteId : catalog.get(courseId).prerequisiteIds()) {
            if (unresolvedCourseIds.contains(prerequisiteId)) {
                List<String> cycle = findCyclePath(prerequisiteId, unresolvedCourseIds, catalog, visiting, visited, stack);
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        stack.removeLast();
        visiting.remove(courseId);
        visited.add(courseId);
        return List.of();
    }

    private List<String> cycleFromStack(String repeatedCourseId, ArrayDeque<String> stack) {
        List<String> cycle = new ArrayList<>();
        boolean insideCycle = false;
        for (String stackedCourseId : stack) {
            if (stackedCourseId.equals(repeatedCourseId)) {
                insideCycle = true;
            }
            if (insideCycle) {
                cycle.add(stackedCourseId);
            }
        }
        cycle.add(repeatedCourseId);
        return cycle;
    }

    private void validateCreditLimit(Set<String> unscheduledCourseIds,
                                     Map<String, Course> catalog,
                                     int maxCreditsPerSemester) {
        for (String courseId : unscheduledCourseIds) {
            Course course = catalog.get(courseId);
            if (course.credits() > maxCreditsPerSemester) {
                throw new UnplannableScheduleException(
                        "Course " + course.id() + " has " + course.credits()
                                + " credits, exceeding the semester limit of " + maxCreditsPerSemester);
            }
        }
    }
}

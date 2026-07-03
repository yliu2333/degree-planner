# Degree Planner

A Spring Boot API that generates semester-by-semester course schedules. Given a student's completed courses and target courses, it models prerequisites as a **DAG**, resolves ordering with **topological sorting**, and greedily fills semesters under real-world constraints (credit limits, term availability).

Originally prototyped at DandyHacks (University of Rochester's hackathon), rebuilt from scratch as a production-style backend.

## How It Works

1. **Prerequisite closure** — collects target courses plus all transitive prerequisites
2. **Cycle detection** — builds a prerequisite DAG; a cycle (bad data) throws `PrerequisiteCycleException` with the exact cycle path, surfaced as HTTP 400
3. **Topological sort** — Kahn's algorithm determines a valid course ordering
4. **Greedy semester filling** — schedules each course into the earliest semester where:
   - all prerequisites are completed in *earlier* semesters
   - total credits stay within `maxCreditsPerSemester`
   - the course is actually offered that term (FALL/SPRING)

Objective: fewest semesters to reach the target courses.

## Architecture

```
src/main/java
├── domain/        Course, StudentProfile, Semester, SemesterPlan, Term
├── repository/    CourseRepository (interface) + MockCourseRepository
├── engine/        SchedulePlanner — pure Java, zero Spring dependencies
└── api/           REST controllers + global exception handling
```

Design decisions:

- **Repository pattern** — the engine depends only on the `CourseRepository` interface. Swapping the JSON-backed mock for MySQL requires zero engine changes.
- **Framework-free engine** — `SchedulePlanner` takes its repository via constructor injection and has no Spring imports, so it's unit-testable in plain JUnit without a Spring context.

## API

### `GET /api/courses`

Returns the full course catalog (40 CS-curriculum courses with multi-level prerequisite chains).

### `POST /api/plan`

Request:

```json
{
  "completedCourseIds": [],
  "targetCourseIds": ["CS491"],
  "maxCreditsPerSemester": 16,
  "startTerm": "FALL"
}
```

Response — an ordered semester plan:

```json
{
  "semesters": [
    {
      "term": "FALL",
      "year": 1,
      "courses": [ { "id": "CS101", "name": "...", "credits": 4 } ],
      "totalCredits": 16
    }
  ]
}
```

Errors: a prerequisite cycle in the data returns `400` with the cycle path in the message.

## Running Locally

Requires Java 17+ and Maven.

```bash
mvn spring-boot:run
# API at http://localhost:8080
curl http://localhost:8080/api/courses
```

## Tests

```bash
mvn test
```

JUnit 5 coverage: topological order correctness, cycle detection, credit-limit boundaries, term-availability constraints, and a full freshman-to-CS491 plan (6 semesters).

## Docker

Multi-stage build (Maven build → slim JRE runtime):

```bash
docker build -t degree-planner .
docker run -p 8080:8080 degree-planner
```

## Tech Stack

Java 17 · Spring Boot 3 · JUnit 5 · Maven · Docker

# Degree Planner

· **Live Demo:** [https://degree-planner-front.onrender.com/] 
· **API:** [https://degree-planner-t0zw.onrender.com/api/courses]

A full-stack degree-planning tool that generates semester-by-semester course schedules. The backend models prerequisites as a **DAG**, resolves ordering with **topological sorting** (Kahn's algorithm), and greedily fills semesters under real-world constraints — credit limits and term availability. The frontend is a Vue 3 single-page app.

Originally prototyped at DandyHacks (University of Rochester's hackathon), rebuilt from scratch as a production-style application.

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
├── src/main/java          Spring Boot backend
│   ├── domain/            Course, StudentProfile, Semester, SemesterPlan, Term
│   ├── repository/        CourseRepository (interface) + MockCourseRepository
│   ├── engine/            SchedulePlanner — pure Java, zero Spring dependencies
│   └── api/               REST controllers + global exception handling
└── frontend/              Vue 3 (Composition API + Vite) SPA
```

Design decisions:

- **Repository pattern** — the engine depends only on the `CourseRepository` interface. Swapping the JSON-backed mock for MySQL requires zero engine changes.
- **Framework-free engine** — `SchedulePlanner` takes its repository via constructor injection and has no Spring imports, so it's unit-testable in plain JUnit without a Spring context.
- **Decoupled deployment** — backend (Docker on Render) and frontend (static site on Render) deploy independently; the frontend targets the API via a build-time env var.

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

**Backend** (Java 17+, Maven):

```bash
mvn spring-boot:run
# API at http://localhost:8080
```

**Frontend** (Node 18+):

```bash
cd frontend
npm install
echo "VITE_API_BASE_URL=http://localhost:8080" > .env
npm run dev
# App at http://localhost:5173
```

## Tests

```bash
mvn test
```

JUnit 5 coverage: topological order correctness, cycle detection, credit-limit boundaries, term-availability constraints, and a full freshman-to-CS491 plan (6 semesters).

## Deployment

- **Backend** — Dockerized (multi-stage: Maven build → slim JRE runtime), deployed as a Render Web Service. CORS origin controlled via `ALLOWED_ORIGIN` env var.
- **Frontend** — built with Vite, deployed as a Render Static Site. API base URL injected at build time via `VITE_API_BASE_URL`.

```bash
docker build -t degree-planner .
docker run -p 8080:8080 degree-planner
```

## Tech Stack

**Backend:** Java 17 · Spring Boot 3 · JUnit 5 · Maven · Docker
**Frontend:** Vue 3 · Vite
**Deployment:** Render (Web Service + Static Site)

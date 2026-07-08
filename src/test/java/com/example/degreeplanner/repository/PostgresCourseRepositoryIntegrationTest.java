package com.example.degreeplanner.repository;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Term;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresCourseRepositoryIntegrationTest {
    @Test
    void loadsSeededCoursesAndPrerequisitesFromPostgres() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("degree_planner")
                .withUsername("degree_planner")
                .withPassword("degree_planner")) {
            postgres.start();

            Assumptions.assumeTrue(
                    canConnectWithJdbc(postgres),
                    "Skipping Testcontainers integration test because Java cannot connect to the mapped Postgres port"
            );

            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()
            );
            dataSource.setDriverClassName("org.postgresql.Driver");

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            CourseRepository courseRepository = new PostgresCourseRepository(new JdbcTemplate(dataSource));

            assertPostgresRepositoryLoadsSeededCourses(courseRepository);
        }
    }

    private void assertPostgresRepositoryLoadsSeededCourses(CourseRepository courseRepository) {
        assertThat(courseRepository).isInstanceOf(PostgresCourseRepository.class);
        assertThat(courseRepository.findAll()).hasSize(40);

        Optional<Course> cs491 = courseRepository.findById("CS491");
        assertThat(cs491).isPresent();
        assertThat(cs491.get().prerequisiteIds()).containsExactly("CS490");
        assertThat(cs491.get().offeredTerms()).containsExactly(Term.SPRING);

        Optional<Course> cs201 = courseRepository.findById("CS201");
        assertThat(cs201).isPresent();
        assertThat(cs201.get().prerequisiteIds()).containsExactly("CS102", "CS110");
        assertThat(cs201.get().offeredTerms()).containsExactlyInAnyOrder(Term.FALL, Term.SPRING);
    }

    private boolean canConnectToMappedPort(PostgreSQLContainer<?> postgres) {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(postgres.getHost(), postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)),
                    (int) Duration.ofSeconds(2).toMillis()
            );
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean canConnectWithJdbc(PostgreSQLContainer<?> postgres) {
        if (!canConnectToMappedPort(postgres)) {
            return false;
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        dataSource.setDriverClassName("org.postgresql.Driver");

        try (Connection ignored = dataSource.getConnection()) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

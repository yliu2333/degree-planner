CREATE TABLE courses (
    id VARCHAR PRIMARY KEY,
    name VARCHAR NOT NULL,
    credits INT NOT NULL CHECK (credits > 0),
    -- offered_terms is comma-separated because terms are a tiny enum-like set used only for Course hydration;
    -- prerequisites are normalized below because course relationships need FK integrity.
    offered_terms VARCHAR NOT NULL
);

CREATE TABLE prerequisites (
    course_id VARCHAR NOT NULL,
    prerequisite_id VARCHAR NOT NULL,
    PRIMARY KEY (course_id, prerequisite_id),
    CONSTRAINT fk_prerequisites_course
        FOREIGN KEY (course_id)
        REFERENCES courses (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_prerequisites_prerequisite
        FOREIGN KEY (prerequisite_id)
        REFERENCES courses (id)
        ON DELETE RESTRICT
);

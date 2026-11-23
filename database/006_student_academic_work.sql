USE erp_db;

-- Student-facing work built on the existing exam, section, and subject schema.
CREATE TABLE IF NOT EXISTS assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    assigned_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (section_id) REFERENCES sections(id),
    INDEX idx_assignments_student_scope (section_id, academic_year, status, due_date)
);

CREATE TABLE IF NOT EXISTS marks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    marks DECIMAL(5,2) NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (exam_id) REFERENCES exams(id),
    UNIQUE(student_id, subject_id, exam_id)
);

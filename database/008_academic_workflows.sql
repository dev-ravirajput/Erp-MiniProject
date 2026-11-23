USE erp_db;

-- Extended academic workflows. Apply after database/007_notifications.sql.
CREATE TABLE IF NOT EXISTS erp_schema_migrations (
    version VARCHAR(100) PRIMARY KEY,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Guardian accounts are separate from student accounts and may link to siblings.
ALTER TABLE users MODIFY role ENUM('ADMIN','TEACHER','STUDENT','PARENT') NOT NULL;
CREATE TABLE IF NOT EXISTS guardians (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    phone VARCHAR(20),
    relationship_label VARCHAR(40),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS guardian_student (
    guardian_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    relationship_label VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (guardian_id, student_id),
    FOREIGN KEY (guardian_id) REFERENCES guardians(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Give each class occurrence an identity; students may have more than one
-- attendance entry for the same subject on a date when there are multiple periods.
CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_subject_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    session_label VARCHAR(40) NOT NULL DEFAULT 'Class',
    start_time TIME NULL,
    end_time TIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_attendance_session (teacher_subject_id, attendance_date, session_label),
    FOREIGN KEY (teacher_subject_id) REFERENCES teacher_subject(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE attendance
    ADD COLUMN session_id BIGINT NULL,
    ADD COLUMN academic_year VARCHAR(20) NULL;

SET @attendance_old_unique = (
    SELECT INDEX_NAME FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance'
      AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') = 'student_id,subject_id,attendance_date'
    LIMIT 1
);
SET @attendance_drop_sql = IF(@attendance_old_unique IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE attendance DROP INDEX `', REPLACE(@attendance_old_unique, '`', '``'), '`'));
PREPARE attendance_drop_stmt FROM @attendance_drop_sql;
EXECUTE attendance_drop_stmt;
DEALLOCATE PREPARE attendance_drop_stmt;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id),
    ADD UNIQUE KEY uq_attendance_session_student (session_id, student_id),
    ADD INDEX idx_attendance_year_section_date (academic_year, section_id, attendance_date);

-- Marks remain editable by teachers, but students only see released results.
ALTER TABLE exams
    ADD COLUMN results_published_at TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN results_published_by BIGINT NULL,
    ADD CONSTRAINT fk_exams_published_by FOREIGN KEY (results_published_by) REFERENCES users(id);

CREATE TABLE IF NOT EXISTS grade_scales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    min_percent DECIMAL(5,2) NOT NULL UNIQUE,
    grade_label VARCHAR(10) NOT NULL,
    status TINYINT(1) NOT NULL DEFAULT 1,
    CHECK (min_percent >= 0 AND min_percent <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO grade_scales(min_percent,grade_label) VALUES
(0,'F'),(60,'D'),(70,'C'),(80,'B'),(90,'A');

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submission_text TEXT,
    stored_filename VARCHAR(255),
    original_filename VARCHAR(255),
    content_type VARCHAR(120),
    file_size BIGINT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grade DECIMAL(5,2) NULL,
    feedback TEXT,
    graded_by BIGINT NULL,
    graded_at TIMESTAMP NULL,
    UNIQUE KEY uq_submission_student (assignment_id, student_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (graded_by) REFERENCES users(id),
    INDEX idx_submissions_assignment (assignment_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE assignments ADD COLUMN max_grade DECIMAL(5,2) NOT NULL DEFAULT 100;

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id BIGINT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_entity (entity_type, entity_id, created_at),
    INDEX idx_audit_actor (actor_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    due_date DATE NOT NULL,
    status TINYINT(1) NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_fee_plans_class_year (class_id, academic_year, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_invoices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    plan_id BIGINT NULL,
    academic_year VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    concession_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    status ENUM('OPEN','VOID') NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (plan_id) REFERENCES fee_plans(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_fee_student_year (student_id, academic_year, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method ENUM('CASH','BANK_TRANSFER','CARD','ONLINE','OTHER') NOT NULL,
    reference_no VARCHAR(100),
    received_by BIGINT NULL,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    FOREIGN KEY (invoice_id) REFERENCES fee_invoices(id),
    FOREIGN KEY (received_by) REFERENCES users(id),
    INDEX idx_fee_payment_invoice (invoice_id, paid_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fee_refunds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reference_no VARCHAR(100),
    processed_by BIGINT NULL,
    notes VARCHAR(500),
    refunded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_id) REFERENCES fee_payments(id),
    FOREIGN KEY (processed_by) REFERENCES users(id),
    INDEX idx_fee_refunds_payment (payment_id, refunded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admission_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    application_no VARCHAR(40) NOT NULL UNIQUE,
    applicant_name VARCHAR(150) NOT NULL,
    applicant_email VARCHAR(150),
    applicant_phone VARCHAR(30),
    date_of_birth DATE NULL,
    requested_class_id BIGINT NULL,
    academic_year VARCHAR(20) NOT NULL,
    notes TEXT,
    status ENUM('RECEIVED','UNDER_REVIEW','ACCEPTED','REJECTED','ENROLLED') NOT NULL DEFAULT 'RECEIVED',
    converted_student_id BIGINT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requested_class_id) REFERENCES classes(id),
    FOREIGN KEY (converted_student_id) REFERENCES students(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_admissions_status_year (status, academic_year, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO erp_schema_migrations(version) VALUES ('008_academic_workflows');

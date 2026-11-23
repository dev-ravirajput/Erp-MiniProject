CREATE TABLE IF NOT EXISTS student_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    roll_no VARCHAR(50),
    academic_year VARCHAR(20) NOT NULL,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE(student_id, academic_year)
);

CREATE TABLE IF NOT EXISTS student_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_class_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    FOREIGN KEY (student_class_id) REFERENCES student_class(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    UNIQUE(student_class_id, subject_id)
);

CREATE TABLE IF NOT EXISTS teacher_subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE(teacher_id, subject_id, section_id, academic_year)
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    section_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status ENUM('PRESENT','ABSENT','LATE','LEAVE') NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (section_id) REFERENCES sections(id),
    UNIQUE(student_id, subject_id, attendance_date)
);

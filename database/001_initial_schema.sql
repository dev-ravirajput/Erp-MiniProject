CREATE DATABASE IF NOT EXISTS erp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    status TINYINT(1) NOT NULL DEFAULT 1,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS teachers (id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL UNIQUE, employee_code VARCHAR(50) NOT NULL UNIQUE, phone VARCHAR(20), qualification VARCHAR(150), joining_date DATE, department VARCHAR(100), FOREIGN KEY (user_id) REFERENCES users(id));
-- Student-specific profile/enrollment data belongs here; login and role stay in users.
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    admission_no VARCHAR(50) NOT NULL UNIQUE,
    roll_no VARCHAR(50),
    phone VARCHAR(20),
    dob DATE,
    gender ENUM('MALE','FEMALE','OTHER'),
    address TEXT,
    admission_date DATE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS classes (id BIGINT PRIMARY KEY AUTO_INCREMENT, class_name VARCHAR(50) NOT NULL, academic_year VARCHAR(20) NOT NULL, status TINYINT(1) NOT NULL DEFAULT 1, UNIQUE(class_name,academic_year));
CREATE TABLE IF NOT EXISTS sections (id BIGINT PRIMARY KEY AUTO_INCREMENT, class_id BIGINT NOT NULL, section_name VARCHAR(20) NOT NULL, FOREIGN KEY (class_id) REFERENCES classes(id), UNIQUE(class_id,section_name));
CREATE TABLE IF NOT EXISTS subjects (id BIGINT PRIMARY KEY AUTO_INCREMENT, subject_code VARCHAR(30) NOT NULL UNIQUE, subject_name VARCHAR(100) NOT NULL, max_marks INT NOT NULL DEFAULT 100, status TINYINT(1) NOT NULL DEFAULT 1);
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
CREATE TABLE IF NOT EXISTS exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_name VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    section_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    exam_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50),
    status TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);
CREATE TABLE IF NOT EXISTS timetable (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    section_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room VARCHAR(50),
    status TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (section_id) REFERENCES sections(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id)
);
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Password for every starter account: Password@123 (SHA-256 for local development only)
INSERT IGNORE INTO users (name,email,password,role) VALUES
('System Administrator','admin@erp.local','ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f','ADMIN'),
('Anita Teacher','teacher@erp.local','ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f','TEACHER'),
('Arjun Student','student@erp.local','ff7bd97b1a7789ddd2775122fd6817f3173672da9f802ceec57f284325bf589f','STUDENT');

USE erp_db;

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

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;

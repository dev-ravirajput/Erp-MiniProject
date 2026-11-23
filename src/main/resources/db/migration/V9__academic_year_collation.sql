-- Keep the new academic-year columns aligned with classes.academic_year.
-- This handles both the legacy utf8mb4_unicode_ci schema and fresh schemas
-- whose MySQL default is utf8mb4_0900_ai_ci.
SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @erp_ay_charset, @erp_ay_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'classes'
  AND COLUMN_NAME = 'academic_year';

SET @erp_ay_ddl = CONCAT(
    'ALTER TABLE fee_plans MODIFY academic_year VARCHAR(20) CHARACTER SET ',
    @erp_ay_charset, ' COLLATE ', @erp_ay_collation, ' NOT NULL'
);
PREPARE erp_ay_stmt FROM @erp_ay_ddl;
EXECUTE erp_ay_stmt;
DEALLOCATE PREPARE erp_ay_stmt;

SET @erp_ay_ddl = CONCAT(
    'ALTER TABLE fee_invoices MODIFY academic_year VARCHAR(20) CHARACTER SET ',
    @erp_ay_charset, ' COLLATE ', @erp_ay_collation, ' NOT NULL'
);
PREPARE erp_ay_stmt FROM @erp_ay_ddl;
EXECUTE erp_ay_stmt;
DEALLOCATE PREPARE erp_ay_stmt;

SET @erp_ay_ddl = CONCAT(
    'ALTER TABLE admission_applications MODIFY academic_year VARCHAR(20) CHARACTER SET ',
    @erp_ay_charset, ' COLLATE ', @erp_ay_collation, ' NOT NULL'
);
PREPARE erp_ay_stmt FROM @erp_ay_ddl;
EXECUTE erp_ay_stmt;
DEALLOCATE PREPARE erp_ay_stmt;

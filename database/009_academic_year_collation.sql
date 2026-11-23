-- Apply after database/008_academic_workflows.sql.
-- Align new workflow tables with the collation used by classes.academic_year.
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

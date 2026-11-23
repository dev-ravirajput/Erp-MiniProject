# ERP System

School management ERP built with Java 21, Jakarta Servlet/JSP, Tomcat 10.1, and MySQL. It includes admin, teacher, student, and parent portals.

## Build and run

1. Install JDK 21, Maven 3, Tomcat 10.1, and MySQL 8.4 or newer.
2. Create an empty database named `erp_db` in MySQL.
3. Apply the versioned schema:

   ```powershell
   mvn flyway:migrate "-Dflyway.url=jdbc:mysql://localhost:3306/erp_db?useSSL=false&serverTimezone=UTC" "-Dflyway.user=YOUR_DB_USER" "-Dflyway.password=YOUR_DB_PASSWORD"
   ```

4. Build the deployable WAR and run the unit checks with `mvn verify`. The artifact is `target/ERPSystem.war`.
5. Configure Tomcat's JVM properties `erp.db.url`, `erp.db.user`, and `erp.db.password`, then deploy the WAR.
6. Optional persistent upload paths can be set with `erp.branding.dir` and `erp.assignments.dir`; defaults are under the Tomcat user's home directory.

The versioned migrations are in `src/main/resources/db/migration`. `database/001` through `database/009` are retained for existing Eclipse/MySQL Workbench installations. For a database already created using scripts 001 through 007, back it up, baseline it at version 7, and migrate through versions 8 and 9:

```powershell
mvn flyway:baseline "-Dflyway.url=jdbc:mysql://localhost:3306/erp_db?useSSL=false&serverTimezone=UTC" "-Dflyway.user=YOUR_DB_USER" "-Dflyway.password=YOUR_DB_PASSWORD" "-Dflyway.baselineVersion=7"
mvn flyway:migrate "-Dflyway.url=jdbc:mysql://localhost:3306/erp_db?useSSL=false&serverTimezone=UTC" "-Dflyway.user=YOUR_DB_USER" "-Dflyway.password=YOUR_DB_PASSWORD"
```

For a database managed manually in MySQL Workbench, apply `database/008_academic_workflows.sql` followed by `database/009_academic_year_collation.sql`. The ninth migration aligns workflow academic-year columns with the existing `classes.academic_year` collation.

## Included workflows

- Admin management for students, teachers, classes, subjects, attendance, exams, timetables, reports, settings, guardians, admissions, fee invoices, payments, and audit history.
- Teacher tools for attendance by class session, assignments and submissions, feedback, marks entry, and result publishing.
- Student and parent views for attendance, assignments, published results, and fee balances.
- Guardian accounts may be linked to multiple students. Admins can enroll accepted applications and roll existing students into a new academic year while retaining prior enrollments.
- Admin student and teacher directories are paginated. Attendance reports can be filtered by academic year and exported to CSV.
- Assignment files are stored outside the deployed web application and are served only after checking the viewer's relationship to the assignment.

## Development

Import the project into Eclipse as a Maven project or use `mvn verify`. CI runs the same build and unit checks on Java 21. The application has development starter accounts in `database/001_initial_schema.sql`; change or remove them for a deployed school database.

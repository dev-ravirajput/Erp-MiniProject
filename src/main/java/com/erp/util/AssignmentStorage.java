package com.erp.util;

import java.nio.file.Path;

/** Persistent storage location for student assignment files. */
public final class AssignmentStorage {
    private AssignmentStorage() { }
    public static Path directory() {
        String configured = System.getProperty("erp.assignments.dir");
        Path root = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "ERPSystem", "assignments") : Path.of(configured.trim());
        return root.toAbsolutePath().normalize();
    }
}

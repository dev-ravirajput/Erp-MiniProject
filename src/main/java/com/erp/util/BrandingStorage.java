package com.erp.util;

import java.nio.file.Path;

/** Persistent storage location for uploaded site branding images. */
public final class BrandingStorage {
    private BrandingStorage() { }

    public static Path directory() {
        String configured = System.getProperty("erp.branding.dir");
        Path directory = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "ERPSystem", "branding")
                : Path.of(configured.trim());
        return directory.toAbsolutePath().normalize();
    }
}

package com.erp.model;

public enum Role {
    ADMIN, TEACHER, STUDENT, PARENT;

    public static Role from(String value) {
        return Role.valueOf(value == null ? "" : value.trim().toUpperCase());
    }
}

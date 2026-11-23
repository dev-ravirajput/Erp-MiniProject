package com.erp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RoleTest {
    @Test void parsesParentRoleCaseInsensitively() {
        assertEquals(Role.PARENT, Role.from("parent"));
    }
}

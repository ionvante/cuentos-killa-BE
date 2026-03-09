package com.forjix.cuentoskilla.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class UserAccessorsCompilationGuardTest {

    @Test
    void shouldExposeSingleDocumentoTipoAndDocumentoNumeroAccessors() {
        assertEquals(1, countMethod(User.class, "getDocumentoTipo"));
        assertEquals(1, countMethod(User.class, "setDocumentoTipo", String.class));
        assertEquals(1, countMethod(User.class, "getDocumentoNumero"));
        assertEquals(1, countMethod(User.class, "setDocumentoNumero", String.class));
    }

    private long countMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Method[] methods = type.getDeclaredMethods();
        return Arrays.stream(methods)
                .filter(method -> method.getName().equals(name))
                .filter(method -> Arrays.equals(method.getParameterTypes(), parameterTypes))
                .count();
    }
}

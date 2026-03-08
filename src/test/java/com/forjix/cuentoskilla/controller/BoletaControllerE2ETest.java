package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.service.BoletaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BoletaControllerE2ETest {

    private StubBoletaService boletaService;
    private BoletaController boletaController;

    @BeforeEach
    void setUp() {
        boletaService = new StubBoletaService();
        boletaController = new BoletaController(boletaService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void descargarBoletaCuandoNoEstaListaRetornaConflict() {
        setAuthenticatedUser(1L, "admin@cuentoskilla.com", Rol.ADMIN);
        boletaService.downloadException = new IllegalStateException("BOLETA_NOT_READY");

        ResponseEntity<?> response = boletaController.descargarBoleta(63L);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ApiResponse<?> body = assertInstanceOf(ApiResponse.class, response.getBody());
        assertEquals("BOLETA_NOT_READY", body.getCode());
        assertEquals(false, body.isSuccess());
    }

    @Test
    void reintentarBoletaComoAdminGeneradaRetornaOk() {
        boletaService.retryResult = new BoletaService.BoletaRetryResult(
                63L, "B001-00000045", "GENERADA", 2, null);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = boletaController.reintentarBoleta(63L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals("GENERADA", response.getBody().getData().get("estadoGeneracion"));
        assertEquals(2, response.getBody().getData().get("intentos"));
    }

    @Test
    void reintentarBoletaComoAdminConErrorRetornaAccepted() {
        boletaService.retryResult = new BoletaService.BoletaRetryResult(
                63L, "B001-00000045", "ERROR", 3, "No se pudo generar el PDF de boleta");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = boletaController.reintentarBoleta(63L);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("SUCCESS", response.getBody().getCode());
        assertEquals("ERROR", response.getBody().getData().get("estadoGeneracion"));
        assertEquals("No se pudo generar el PDF de boleta", response.getBody().getData().get("ultimoError"));
    }

    private void setAuthenticatedUser(Long id, String email, Rol rol) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(rol);

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static class StubBoletaService extends BoletaService {
        RuntimeException downloadException;
        BoletaRetryResult retryResult;

        StubBoletaService() {
            super(null, null, null);
        }

        @Override
        public void init() {
            // no-op
        }

        @Override
        public BoletaArchivo obtenerBoletaParaDescarga(Long orderId, Long requesterId, boolean isAdmin) {
            if (downloadException != null) {
                throw downloadException;
            }
            throw new IllegalStateException("BOLETA_NOT_READY");
        }

        @Override
        public BoletaRetryResult reintentarGeneracionBoleta(Long orderId) {
            return retryResult;
        }
    }
}

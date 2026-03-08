package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Maestro;
import com.forjix.cuentoskilla.model.DTOs.FacturacionParametroRequest;
import com.forjix.cuentoskilla.repository.MaestroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FacturacionConfigServiceTest {

    private MaestroRepository maestroRepository;
    private FacturacionConfigService service;

    @BeforeEach
    void setUp() {
        maestroRepository = Mockito.mock(MaestroRepository.class);
        service = new FacturacionConfigService(maestroRepository);
    }

    @Test
    void actualizarParametroValidaRuc() {
        Maestro ruc = new Maestro();
        ruc.setCodigo("EMPRESA_RUC");
        ruc.setValor("12345678901");
        ruc.setEstado(true);

        Mockito.when(maestroRepository.findByCodigo("EMPRESA_RUC")).thenReturn(Optional.of(ruc));

        FacturacionParametroRequest request = new FacturacionParametroRequest();
        request.setValor("123");

        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarParametro("EMPRESA_RUC", request));
    }

    @Test
    void tomarSiguienteCorrelativoIncrementa() {
        Maestro correlativo = new Maestro();
        correlativo.setCodigo("BOLETA_CORRELATIVO_ACTUAL");
        correlativo.setValor("7");
        correlativo.setEstado(true);

        Mockito.when(maestroRepository.findByCodigoForUpdate("BOLETA_CORRELATIVO_ACTUAL"))
                .thenReturn(Optional.of(correlativo));
        Mockito.when(maestroRepository.save(Mockito.any(Maestro.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int next = service.tomarSiguienteCorrelativoBoleta();

        assertEquals(8, next);
        assertEquals("8", correlativo.getValor());
    }
}

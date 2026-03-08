package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.repository.BoletaRepository;
import com.forjix.cuentoskilla.repository.MaestroRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoletaServiceTest {

    private BoletaRepository boletaRepository;
    private BoletaService service;

    @BeforeEach
    void setUp() {
        boletaRepository = Mockito.mock(BoletaRepository.class);
        OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
        MaestroRepository maestroRepository = Mockito.mock(MaestroRepository.class);
        FacturacionConfigService facturacionConfigService = new FacturacionConfigService(maestroRepository);
        service = new BoletaService(boletaRepository, orderRepository, facturacionConfigService);
    }

    @Test
    void generarBoletaSiCorrespondeEsIdempotenteSiYaExiste() {
        Boleta boleta = new Boleta();
        boleta.setId(99L);

        Mockito.when(boletaRepository.findByOrder_Id(1L)).thenReturn(Optional.of(boleta));

        Boleta result = service.generarBoletaSiCorresponde(1L);

        assertEquals(99L, result.getId());
        Mockito.verify(boletaRepository).findByOrder_Id(1L);
        Mockito.verifyNoMoreInteractions(boletaRepository);
    }
}

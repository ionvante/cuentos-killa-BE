package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.DTOs.StatsDto;
import com.forjix.cuentoskilla.model.DTOs.TimeCount;
import com.forjix.cuentoskilla.repository.StatsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;

public class StatsServiceTest {

    @Test
    public void testGetStats7Days() {
        StatsRepository repo = Mockito.mock(StatsRepository.class);
        LocalDateTime now = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Mockito.when(repo.countCuentosByInterval(any(), any(), anyString()))
                .thenReturn(List.of(new TimeCount(now.minusDays(1), 2L)));
        Mockito.when(repo.countOrdersByStatusAndInterval(any(), any(), any(), anyString()))
                .thenReturn(List.of(new TimeCount(now.minusDays(1), 1L)));
        Mockito.when(repo.countUsersByInterval(any(), any(), anyString()))
                .thenReturn(List.of(new TimeCount(now.minusDays(1), 3L)));
        StatsService service = new StatsService(repo);
        StatsDto dto = service.getStats("7");
        assertEquals(7, dto.getCuentos().size());
        assertEquals(7, dto.getPedidos().size());
        assertEquals(7, dto.getUsuarios().size());
        assertEquals(2L, dto.getCuentos().get(5));
        assertEquals(1L, dto.getPedidos().get(5));
        assertEquals(3L, dto.getUsuarios().get(5));
    }
}

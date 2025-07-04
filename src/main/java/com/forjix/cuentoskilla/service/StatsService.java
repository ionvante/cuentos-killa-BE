package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.DTOs.StatsDto;
import com.forjix.cuentoskilla.model.DTOs.TimeCount;
import com.forjix.cuentoskilla.repository.StatsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final StatsRepository repo;

    public StatsService(StatsRepository repo) {
        this.repo = repo;
    }

    private static class RangeInfo {
        LocalDateTime start;
        LocalDateTime end;
        String unit;
        ChronoUnit step;
    }

    private RangeInfo parseRange(String range) {
        RangeInfo info = new RangeInfo();
        LocalDate today = LocalDate.now();
        if (range != null && range.endsWith("m")) {
            int months = Integer.parseInt(range.substring(0, range.length() - 1));
            info.unit = "month";
            info.step = ChronoUnit.MONTHS;
            info.end = today.withDayOfMonth(1).atStartOfDay();
            info.start = info.end.minusMonths(months - 1);
        } else {
            int days = Integer.parseInt(range);
            if (days >= 365) {
                info.unit = "month";
                info.step = ChronoUnit.MONTHS;
                info.end = today.withDayOfMonth(1).atStartOfDay();
                info.start = info.end.minusMonths(days / 30L - 1);
            } else {
                info.unit = "day";
                info.step = ChronoUnit.DAYS;
                info.end = today.atStartOfDay();
                info.start = info.end.minusDays(days - 1);
            }
        }
        return info;
    }

    private List<Long> fill(List<TimeCount> counts, RangeInfo info) {
        Map<LocalDateTime, Long> map = new HashMap<>();
        for (TimeCount tc : counts) {
            map.put(tc.getTime(), tc.getCount());
        }
        List<Long> result = new ArrayList<>();
        LocalDateTime point = info.start;
        while (!point.isAfter(info.end)) {
            result.add(map.getOrDefault(point, 0L));
            point = point.plus(1, info.step);
        }
        return result;
    }

    public StatsDto getStats(String range) {
        RangeInfo info = parseRange(range);
        List<TimeCount> cuentos = repo.countCuentosByInterval(info.start, info.end, info.unit);
        List<TimeCount> pedidos = repo.countOrdersByStatusAndInterval(OrderStatus.valueOf("EN_PROCESO"), info.start, info.end, info.unit);
        List<TimeCount> usuarios = repo.countUsersByInterval(info.start, info.end, info.unit);

        StatsDto dto = new StatsDto();
        dto.setCuentos(fill(cuentos, info));
        dto.setPedidos(fill(pedidos, info));
        dto.setUsuarios(fill(usuarios, info));
        return dto;
    }
}

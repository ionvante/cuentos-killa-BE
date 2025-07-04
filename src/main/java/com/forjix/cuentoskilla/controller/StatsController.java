package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.StatsDto;
import com.forjix.cuentoskilla.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Aggregated statistics", parameters = {
            @Parameter(name = "range", description = "Number of days or 'Nm' for last N months")})
    public StatsDto getStats(@RequestParam String range) {
        return service.getStats(range);
    }
}

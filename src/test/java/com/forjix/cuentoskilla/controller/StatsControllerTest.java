package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.JwtUtil;
import com.forjix.cuentoskilla.model.DTOs.StatsDto;
import com.forjix.cuentoskilla.service.StatsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collections;

@WebMvcTest(StatsController.class)
public class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatsService service;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    public void testGetStats() throws Exception {
        StatsDto dto = new StatsDto();
        dto.setCuentos(Collections.emptyList());
        dto.setPedidos(Collections.emptyList());
        dto.setUsuarios(Collections.emptyList());
        Mockito.when(service.getStats("7")).thenReturn(dto);
        Mockito.when(jwtUtil.validateToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(jwtUtil.extractUsername(Mockito.anyString())).thenReturn("admin");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/stats")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin"))
                        .param("range", "7"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.cuentos").exists());
    }
}

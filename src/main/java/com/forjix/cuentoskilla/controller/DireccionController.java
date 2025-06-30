package com.forjix.cuentoskilla.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;
import com.forjix.cuentoskilla.service.DireccionService;

@RestController
@RequestMapping("/api/direcciones")
@CrossOrigin
public class DireccionController {

    private final DireccionService service;

    public DireccionController(DireccionService service) {
        this.service = service;
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Direccion> listarPorUsuario(@PathVariable Long usuarioId) {
        return service.obtenerPorUsuario(usuarioId);
    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody DireccionDTO dto) {
        try {
            Direccion saved = service.guardar(dto);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

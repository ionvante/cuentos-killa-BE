package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.service.CuentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/cuentos")
@CrossOrigin
public class CuentoController {

    private static final Logger logger = LoggerFactory.getLogger(CuentoController.class);
    private final CuentoRepository cuentoRepository;
    private final CuentoService cuentoService;
    private final ObjectMapper objectMapper;

    public CuentoController(CuentoRepository cuentoRepository, CuentoService cuentoService, ObjectMapper objectMapper) {
        this.cuentoRepository = cuentoRepository;
        this.cuentoService = cuentoService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<Cuento> getAll() {
        logger.info("GetMapping getAll() ejecutado");
        return cuentoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cuento> getByIdCuentos(@PathVariable Long id) {
        logger.info("GetMapping getByIdCuentos() ejecutado");
        return cuentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<Cuento> create(
            @RequestPart("cuento") String cuentoJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        logger.info("PostMapping create() ejecutado con multipart/form-data");
        try {
            Cuento cuento = objectMapper.readValue(cuentoJson, Cuento.class);
            return ResponseEntity.ok(cuentoService.save(cuento, file));
        } catch (IOException e) {
            logger.error("Error parsing cuento JSON", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<Cuento> update(
            @PathVariable Long id,
            @RequestPart("cuento") String cuentoJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        logger.info("PutMapping update() ejecutado con multipart/form-data");
        try {
            Cuento cuento = objectMapper.readValue(cuentoJson, Cuento.class);
            return cuentoService.update(id, cuento, file)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IOException e) {
            logger.error("Error parsing cuento JSON", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cuento> updateEstado(@PathVariable Long id, @RequestBody Cuento cuento) {
        logger.info("PutMapping updateEstado() ejecutado");
        return cuentoRepository.findById(id)
                .map(existing -> {
                    existing.setHabilitado(cuento.isHabilitado());
                    return ResponseEntity.ok(cuentoRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        logger.info("DeleteMapping delete() ejecutado");
        cuentoRepository.deleteById(id);
    }
}

package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.service.CuentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentos")
@CrossOrigin
public class CuentoController {

    private static final Logger logger = LoggerFactory.getLogger(CuentoController.class);
    private final CuentoRepository cuentoRepository;
    public CuentoController(CuentoService cuentoService) {
        this.cuentoService = cuentoService;
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

    @PostMapping
    public Cuento create(@RequestBody Cuento cuento) {
        logger.info("PostMapping create() ejecutado");
        return cuentoRepository.save(cuento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cuento> update(@PathVariable Long id, @RequestBody Cuento cuento) {
        logger.info("PutMapping update() ejecutado");
        return cuentoRepository.findById(id)
                .map(existing -> {
                    cuento.setId(id);
                    return ResponseEntity.ok(cuentoRepository.save(cuento));
                })
                .orElse(ResponseEntity.notFound().build());
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

package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.repository.CuentoRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentos")
@CrossOrigin
public class CuentoController {

    private final CuentoRepository cuentoRepository;

    public CuentoController(CuentoRepository cuentoRepository) {
        this.cuentoRepository = cuentoRepository;
    }

    @GetMapping
    public List<Cuento> getAll() {
        System.out.println("GetMapping getAll() ejecutado");
        return cuentoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cuento> getByIdCuentos(@PathVariable Long id) {
        System.out.println("GetMapping getByIdCuentos() ejecutado");
        return cuentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }  

    @PostMapping
    public Cuento create(@RequestBody Cuento cuento) {
        System.out.println("PostMapping create() ejecutado");
        return cuentoRepository.save(cuento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cuento> update(@PathVariable Long id, @RequestBody Cuento cuento) {
        System.out.println("PutMapping update() ejecutado");
        return cuentoRepository.findById(id)
                .map(existing -> {
                    cuento.setId(id);
                    return ResponseEntity.ok(cuentoRepository.save(cuento));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cuento> updateEstado(@PathVariable Long id, @RequestBody Cuento cuento) {
        System.out.println("PutMapping updateEstado() ejecutado");
        return cuentoRepository.findById(id)
                .map(existing -> {
                    existing.setHabilitado(cuento.isHabilitado());
                    return ResponseEntity.ok(cuentoRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        System.out.println("DeleteMapping delete() ejecutado");
        cuentoRepository.deleteById(id);
    }
}

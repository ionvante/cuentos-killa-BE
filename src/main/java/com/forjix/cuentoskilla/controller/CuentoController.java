package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.service.CuentoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentos")
@CrossOrigin
public class CuentoController {

    private final CuentoService cuentoService;

    public CuentoController(CuentoService cuentoService) {
        this.cuentoService = cuentoService;
    }

    @GetMapping
    public List<Cuento> getAll() {
        System.out.println("GetMapping getAll() ejecutado");
        return cuentoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cuento> getByIdCuentos(@PathVariable Long id) {
        System.out.println("GetMapping getByIdCuentos() ejecutado");
        return cuentoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }  

    @PostMapping
    public Cuento create(@RequestBody Cuento cuento) {
        System.out.println("PostMapping create() ejecutado");
        return cuentoService.save(cuento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cuento> update(@PathVariable Long id, @RequestBody Cuento cuento) {
        System.out.println("PutMapping update() ejecutado");
        return cuentoService.update(id, cuento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cuento> updateEstado(@PathVariable Long id, @RequestBody Cuento cuento) {
        System.out.println("PutMapping updateEstado() ejecutado");
        return cuentoService.updateEstado(id, cuento)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        System.out.println("DeleteMapping delete() ejecutado");
        cuentoService.delete(id);
    }
}

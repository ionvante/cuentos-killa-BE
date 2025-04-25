package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.repository.CuentoRepository;
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
        return cuentoRepository.findAll();
    }

    @PostMapping
    public Cuento create(@RequestBody Cuento cuento) {
        return cuentoRepository.save(cuento);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cuentoRepository.deleteById(id);
    }
}

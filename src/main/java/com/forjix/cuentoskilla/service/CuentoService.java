package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CuentoService {
    private final CuentoRepository repo;

    public CuentoService(CuentoRepository repo) {
        this.repo = repo;
    }

    public List<Cuento> findAll() {
        return repo.findAll();
    }

    public Optional<Cuento> findById(Long id) {
        return repo.findById(id);
    }

    public Cuento save(Cuento cuento) {
        return repo.save(cuento);
    }

    public Optional<Cuento> update(Long id, Cuento cuento) {
        return repo.findById(id)
                .map(existing -> {
                    cuento.setId(id);
                    return repo.save(cuento);
                });
    }

    public Optional<Cuento> updateEstado(Long id, Cuento cuento) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setHabilitado(cuento.isHabilitado());
                    return repo.save(existing);
                });
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}

package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Maestro;
import com.forjix.cuentoskilla.repository.MaestroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MaestroService {

    @Autowired
    private MaestroRepository maestroRepository;

    public List<Maestro> obtenerTodos() {
        return maestroRepository.findAll();
    }

    public List<Maestro> obtenerPorGrupo(String grupo) {
        return maestroRepository.findByGrupoAndEstadoTrue(grupo);
    }

    public List<Maestro> obtenerTodosPorGrupoAdmin(String grupo) {
        return maestroRepository.findByGrupo(grupo);
    }

    public Maestro crearMaestro(Maestro maestro) {
        return maestroRepository.save(maestro);
    }

    public Maestro actualizarMaestro(Long id, Maestro detalles) {
        Maestro maestro = maestroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maestro no encontrado: " + id));

        maestro.setGrupo(detalles.getGrupo());
        maestro.setCodigo(detalles.getCodigo());
        maestro.setValor(detalles.getValor());
        maestro.setDescripcion(detalles.getDescripcion());
        maestro.setEstado(detalles.getEstado());

        return maestroRepository.save(maestro);
    }

    public void eliminarMaestro(Long id) {
        Maestro maestro = maestroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maestro no encontrado: " + id));
        maestro.setEstado(false);
        maestroRepository.save(maestro);
    }
}

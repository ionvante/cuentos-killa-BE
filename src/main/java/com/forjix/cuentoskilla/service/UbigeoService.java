package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Departamento;
import com.forjix.cuentoskilla.model.Distrito;
import com.forjix.cuentoskilla.model.Provincia;
import com.forjix.cuentoskilla.repository.DepartamentoRepository;
import com.forjix.cuentoskilla.repository.DistritoRepository;
import com.forjix.cuentoskilla.repository.ProvinciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UbigeoService {

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private ProvinciaRepository provinciaRepository;

    @Autowired
    private DistritoRepository distritoRepository;

    public List<Departamento> obtenerTodosDepartamentos() {
        return departamentoRepository.findAll();
    }

    public List<Provincia> obtenerProvinciasPorDepartamento(String departamentoId) {
        return provinciaRepository.findByDepartamentoId(departamentoId);
    }

    public List<Distrito> obtenerDistritosPorProvincia(String provinciaId) {
        return distritoRepository.findByProvinciaId(provinciaId);
    }
}

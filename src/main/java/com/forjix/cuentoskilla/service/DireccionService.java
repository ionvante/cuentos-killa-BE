package com.forjix.cuentoskilla.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;

@Service
public class DireccionService {

    private final DireccionRepository direccionRepo;
    private final UserRepository userRepo;

    public DireccionService(DireccionRepository direccionRepo, UserRepository userRepo) {
        this.direccionRepo = direccionRepo;
        this.userRepo = userRepo;
    }

    public List<Direccion> obtenerPorUsuario(Long usuarioId) {
        return direccionRepo.findByUsuarioId(usuarioId);
    }

    public Direccion guardar(DireccionDTO dto) {
        User user = userRepo.findById(dto.getUsuarioId())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Direccion direccion = new Direccion();
        direccion.setCalle(dto.getCalle());
        direccion.setCiudad(dto.getCiudad());
        direccion.setDepartamento(dto.getDepartamento());
        direccion.setProvincia(dto.getProvincia());
        direccion.setDistrito(dto.getDistrito());
        direccion.setReferencia(dto.getReferencia());
        direccion.setCodigoPostal(dto.getCodigoPostal());
        direccion.setEsPrincipal(dto.isEsPrincipal());
        direccion.setEsFacturacion(dto.isEsFacturacion());
        direccion.setUsuario(user);

        return direccionRepo.save(direccion);
    }

    public void eliminar(Long id) {
        direccionRepo.deleteById(id);
    }
}

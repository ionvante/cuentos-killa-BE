package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DireccionService {

    private static final Set<String> TIPOS_DIRECCION_VALIDOS = Set.of("CASA", "TRABAJO", "FAMILIAR", "OTRO");

    private final DireccionRepository direccionRepo;
    private final UserRepository userRepo;

    public DireccionService(DireccionRepository direccionRepo, UserRepository userRepo) {
        this.direccionRepo = direccionRepo;
        this.userRepo = userRepo;
    }

    public List<Direccion> obtenerPorUsuario(Long usuarioId, Long requesterId, boolean isAdmin) {
        validarAccesoUsuario(usuarioId, requesterId, isAdmin);
        return direccionRepo.findByUsuarioId(usuarioId);
    }

    public Direccion guardar(DireccionDTO dto, Long requesterId, boolean isAdmin) {
        User user = resolverUsuarioDestino(dto.getUsuarioId(), requesterId, isAdmin);

        Direccion direccion = new Direccion();
        aplicarCambios(direccion, dto);
        direccion.setUsuario(user);

        return direccionRepo.save(direccion);
    }

    public void eliminar(Long id, Long requesterId, boolean isAdmin) {
        Direccion direccion = obtenerDireccionAutorizada(id, requesterId, isAdmin);
        direccionRepo.delete(direccion);
    }

    public Direccion actualizar(Long id, DireccionDTO dto, Long requesterId, boolean isAdmin) {
        Direccion direccion = obtenerDireccionAutorizada(id, requesterId, isAdmin);
        aplicarCambios(direccion, dto);
        return direccionRepo.save(direccion);
    }

    private Direccion obtenerDireccionAutorizada(Long id, Long requesterId, boolean isAdmin) {
        Direccion direccion = direccionRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Direccion no encontrada"));
        Long ownerId = direccion.getUsuario() != null ? direccion.getUsuario().getId() : null;
        validarAccesoUsuario(ownerId, requesterId, isAdmin);
        return direccion;
    }

    private User resolverUsuarioDestino(Long requestedUserId, Long requesterId, boolean isAdmin) {
        Long targetUserId = requesterId;
        if (isAdmin && requestedUserId != null) {
            targetUserId = requestedUserId;
        }
        return userRepo.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));
    }

    private void validarAccesoUsuario(Long ownerId, Long requesterId, boolean isAdmin) {
        if (ownerId == null) {
            throw new NoSuchElementException("Usuario no encontrado");
        }
        if (!isAdmin && !ownerId.equals(requesterId)) {
            throw new SecurityException("ACCESS_DENIED");
        }
    }

    private void aplicarCambios(Direccion direccion, DireccionDTO dto) {
        direccion.setCalle(dto.getCalle());
        direccion.setCiudad(dto.getCiudad());
        direccion.setDepartamento(dto.getDepartamento());
        direccion.setProvincia(dto.getProvincia());
        direccion.setDistrito(dto.getDistrito());
        direccion.setTipoDireccion(normalizarTipoDireccion(dto.getTipoDireccion()));
        direccion.setReferencia(dto.getReferencia());
        direccion.setCodigoPostal(dto.getCodigoPostal());
        direccion.setEsPrincipal(dto.isEsPrincipal());
        direccion.setEsFacturacion(dto.isEsFacturacion());
    }

    private String normalizarTipoDireccion(String tipoDireccion) {
        if (tipoDireccion == null || tipoDireccion.isBlank()) {
            return null;
        }
        String normalizado = tipoDireccion.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_DIRECCION_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("TIPO_DIRECCION_INVALIDO");
        }
        return normalizado;
    }
}

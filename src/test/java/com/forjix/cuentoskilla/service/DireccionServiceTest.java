package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class DireccionServiceTest {

    private DireccionRepository direccionRepository;
    private UserRepository userRepository;
    private DireccionService direccionService;

    @BeforeEach
    void setUp() {
        direccionRepository = Mockito.mock(DireccionRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        direccionService = new DireccionService(direccionRepository, userRepository);
    }

    @Test
    void obtenerPorUsuarioRejectsForeignUserForNonAdmin() {
        assertThrows(SecurityException.class, () -> direccionService.obtenerPorUsuario(2L, 1L, false));
    }

    @Test
    void guardarIgnoresUsuarioIdForNormalUsers() {
        User requester = buildUser(1L);
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        Mockito.when(direccionRepository.save(any(Direccion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DireccionDTO dto = new DireccionDTO();
        dto.setUsuarioId(99L);
        dto.setTipoDireccion("casa");
        dto.setCalle("Calle 1");

        Direccion saved = direccionService.guardar(dto, 1L, false);

        assertSame(requester, saved.getUsuario());
        assertEquals("CASA", saved.getTipoDireccion());
        assertEquals("Calle 1", saved.getCalle());
    }

    @Test
    void actualizarRejectsForeignAddressForNonAdmin() {
        Direccion direccion = new Direccion();
        direccion.setId(5L);
        direccion.setUsuario(buildUser(2L));
        Mockito.when(direccionRepository.findById(5L)).thenReturn(Optional.of(direccion));

        assertThrows(SecurityException.class,
                () -> direccionService.actualizar(5L, new DireccionDTO(), 1L, false));
    }

    @Test
    void guardarRejectsInvalidAddressType() {
        User requester = buildUser(1L);
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(requester));

        DireccionDTO dto = new DireccionDTO();
        dto.setTipoDireccion("PLAYA");

        assertThrows(IllegalArgumentException.class, () -> direccionService.guardar(dto, 1L, false));
    }

    @Test
    void obtenerPorUsuarioAllowsAdminToSeeOtherUsersDirections() {
        Direccion direccion = new Direccion();
        direccion.setId(8L);
        Mockito.when(direccionRepository.findByUsuarioId(2L)).thenReturn(List.of(direccion));

        List<Direccion> direcciones = direccionService.obtenerPorUsuario(2L, 1L, true);

        assertEquals(1, direcciones.size());
        assertEquals(8L, direcciones.get(0).getId());
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}

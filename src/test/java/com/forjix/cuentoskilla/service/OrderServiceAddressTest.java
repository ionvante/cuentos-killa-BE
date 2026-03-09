package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
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

class OrderServiceAddressTest {

    private OrderRepository orderRepository;
    private CuentoRepository cuentoRepository;
    private UserRepository userRepository;
    private DireccionRepository direccionRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        cuentoRepository = Mockito.mock(CuentoRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        direccionRepository = Mockito.mock(DireccionRepository.class);

        Mockito.when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(cuentoRepository.findById(5L)).thenReturn(Optional.of(buildCuento()));

        orderService = new OrderService(
                orderRepository,
                cuentoRepository,
                userRepository,
                direccionRepository,
                new MercadoPagoService(),
                new StubEmailService(),
                new StubBoletaService()
        );
    }

    @Test
    void saveUsesAuthenticatedUserAndSnapshotFromPayload() {
        User authenticatedUser = buildUser(7L, "user@example.com");
        Direccion direccion = buildDireccion(10L, authenticatedUser);
        Mockito.when(direccionRepository.findById(10L)).thenReturn(Optional.of(direccion));

        PedidoDTO pedido = buildPedidoBase();
        pedido.setUserId(999L);
        pedido.setDireccionId(10L);
        pedido.setTipoDireccion("OTRO");
        pedido.setDepartamento("Lima");
        pedido.setProvincia("Lima");
        pedido.setDistrito("Barranco");
        pedido.setCalle("Jr. Nueva Vida 456");
        pedido.setReferencia("Frente al parque");
        pedido.setCodigoPostal("15063");
        pedido.setDireccion("Jr. Nueva Vida 456, Frente al parque, Barranco, Lima, Lima, 15063");

        Order order = orderService.save(pedido, authenticatedUser);

        assertSame(authenticatedUser, order.getUser());
        assertEquals(10L, order.getDireccionId());
        assertEquals("OTRO", order.getTipoDireccion());
        assertEquals("Barranco", order.getDistrito());
        assertEquals("Jr. Nueva Vida 456", order.getCalle());
        assertEquals("Jr. Nueva Vida 456, Frente al parque, Barranco, Lima, Lima, 15063", order.getDireccion());
    }

    @Test
    void saveRejectsForeignDireccionId() {
        User authenticatedUser = buildUser(7L, "user@example.com");
        User otherUser = buildUser(8L, "other@example.com");
        Mockito.when(direccionRepository.findById(10L)).thenReturn(Optional.of(buildDireccion(10L, otherUser)));

        PedidoDTO pedido = buildPedidoBase();
        pedido.setDireccionId(10L);

        assertThrows(SecurityException.class, () -> orderService.save(pedido, authenticatedUser));
    }

    @Test
    void saveFallsBackToSavedDireccionWhenSnapshotIsPartial() {
        User authenticatedUser = buildUser(7L, "user@example.com");
        Direccion direccion = buildDireccion(10L, authenticatedUser);
        Mockito.when(direccionRepository.findById(10L)).thenReturn(Optional.of(direccion));

        PedidoDTO pedido = buildPedidoBase();
        pedido.setDireccionId(10L);
        pedido.setDireccion("Texto visible en checkout");

        Order order = orderService.save(pedido, authenticatedUser);

        assertEquals("CASA", order.getTipoDireccion());
        assertEquals("Av. Siempre Viva 742", order.getCalle());
        assertEquals("Miraflores", order.getDistrito());
        assertEquals("Texto visible en checkout", order.getDireccion());
    }

    @Test
    void saveStoresManualSnapshotWithoutDireccionId() {
        User authenticatedUser = buildUser(7L, "user@example.com");

        PedidoDTO pedido = buildPedidoBase();
        pedido.setTipoDireccion("FAMILIAR");
        pedido.setDepartamento("Cusco");
        pedido.setProvincia("Cusco");
        pedido.setDistrito("Wanchaq");
        pedido.setCalle("Calle Manual 123");
        pedido.setReferencia("Interior 4");
        pedido.setCodigoPostal("08002");

        Order order = orderService.save(pedido, authenticatedUser);

        assertEquals(null, order.getDireccionId());
        assertEquals("FAMILIAR", order.getTipoDireccion());
        assertEquals("Calle Manual 123", order.getCalle());
        assertEquals("Calle Manual 123, Interior 4, Wanchaq, Cusco, Cusco, 08002", order.getDireccion());
    }

    private PedidoDTO buildPedidoBase() {
        PedidoItemDTO item = new PedidoItemDTO();
        item.setCuentoId(5L);
        item.setCantidad(2);

        PedidoDTO pedido = new PedidoDTO();
        pedido.setItems(List.of(item));
        pedido.setCorreoUsuario("user@example.com");
        return pedido;
    }

    private Cuento buildCuento() {
        Cuento cuento = new Cuento();
        cuento.setId(5L);
        cuento.setTitulo("Cuento de prueba");
        cuento.setImagenUrl("cover.jpg");
        cuento.setPrecio(25.0);
        return cuento;
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private Direccion buildDireccion(Long id, User user) {
        Direccion direccion = new Direccion();
        direccion.setId(id);
        direccion.setUsuario(user);
        direccion.setTipoDireccion("CASA");
        direccion.setDepartamento("Lima");
        direccion.setProvincia("Lima");
        direccion.setDistrito("Miraflores");
        direccion.setCalle("Av. Siempre Viva 742");
        direccion.setReferencia("Dpto 201");
        direccion.setCodigoPostal("15074");
        return direccion;
    }

    static class StubBoletaService extends BoletaService {
        StubBoletaService() {
            super(null, null, null);
        }

        @Override
        public void init() {
        }
    }

    static class StubEmailService extends EmailService {
    }
}

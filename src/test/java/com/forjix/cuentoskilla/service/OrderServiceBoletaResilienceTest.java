package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Boleta;
import com.forjix.cuentoskilla.model.BoletaGeneracionEstado;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.DireccionRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderServiceBoletaResilienceTest {

    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private DireccionRepository direccionRepository;
    private StubBoletaService boletaService;
    private StubEmailService emailService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        CuentoRepository cuentoRepository = Mockito.mock(CuentoRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        direccionRepository = Mockito.mock(DireccionRepository.class);
        MercadoPagoService mercadoPagoService = new MercadoPagoService();
        emailService = new StubEmailService();
        boletaService = new StubBoletaService();

        orderService = new OrderService(
                orderRepository,
                cuentoRepository,
                userRepository,
                direccionRepository,
                mercadoPagoService,
                emailService,
                boletaService
        );
    }

    @Test
    void updateOrderStatusNoReviertePagoVerificadoSiBoletaLanzaExcepcion() {
        Long orderId = 63L;
        Long adminId = 1L;

        Order order = new Order();
        order.setId(orderId);
        order.setEstado(OrderStatus.PAGO_ENVIADO);

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Rol.ADMIN);

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        boletaService.throwOnGenerate = true;

        assertDoesNotThrow(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.PAGO_VERIFICADO, null, adminId));

        assertEquals(OrderStatus.PAGO_VERIFICADO, order.getEstado());
        Mockito.verify(orderRepository).save(order);
        assertEquals(order, emailService.lastOrder);
        assertEquals(OrderStatus.PAGO_VERIFICADO, emailService.lastStatus);
    }

    @Test
    void updateOrderStatusMantienePagoVerificadoSiBoletaQuedaEnError() {
        Long orderId = 64L;
        Long adminId = 1L;

        Order order = new Order();
        order.setId(orderId);
        order.setEstado(OrderStatus.PAGO_ENVIADO);

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Rol.ADMIN);

        Boleta boletaError = new Boleta();
        boletaError.setEstadoGeneracion(BoletaGeneracionEstado.ERROR);
        boletaError.setIntentos(2);
        boletaError.setUltimoError("No se pudo generar el PDF de boleta");

        boletaService.result = boletaError;

        Mockito.when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                orderService.updateOrderStatus(orderId, OrderStatus.PAGO_VERIFICADO, null, adminId));

        assertEquals(OrderStatus.PAGO_VERIFICADO, order.getEstado());
        Mockito.verify(orderRepository).save(order);
        assertEquals(order, emailService.lastOrder);
        assertEquals(OrderStatus.PAGO_VERIFICADO, emailService.lastStatus);
    }

    static class StubBoletaService extends BoletaService {
        boolean throwOnGenerate;
        Boleta result;

        StubBoletaService() {
            super(null, null, null);
        }

        @Override
        public void init() {
            // no-op
        }

        @Override
        public Boleta generarBoletaSiCorresponde(Long orderId) {
            if (throwOnGenerate) {
                throw new RuntimeException("fallo al generar boleta");
            }
            if (result != null) {
                return result;
            }
            Boleta ok = new Boleta();
            ok.setEstadoGeneracion(BoletaGeneracionEstado.GENERADA);
            ok.setIntentos(1);
            return ok;
        }
    }

    static class StubEmailService extends EmailService {
        Order lastOrder;
        OrderStatus lastStatus;

        @Override
        public void enviarNotificacionCambioEstado(Order order, OrderStatus nuevoEstado) {
            this.lastOrder = order;
            this.lastStatus = nuevoEstado;
        }
    }
}

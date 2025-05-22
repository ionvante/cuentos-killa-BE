package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.model.DTOs.PedidoItemDTO;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.repository.OrderRepository;
import com.forjix.cuentoskilla.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CuentoRepository cuentoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void testSaveOrderWithTipoDePago() {
        // 1. Create a PedidoDTO and set values
        PedidoDTO pedidoDTO = new PedidoDTO();
        pedidoDTO.setTipoDePago("Credit Card");
        pedidoDTO.setEstado("Generado");
        pedidoDTO.setCorreoUsuario("test@example.com"); // Assuming user will be found by email

        List<PedidoItemDTO> itemDTOs = new ArrayList<>();
        PedidoItemDTO itemDTO = new PedidoItemDTO();
        itemDTO.setCuentoId(1L);
        itemDTO.setCantidad(2);
        itemDTOs.add(itemDTO);
        pedidoDTO.setItems(itemDTOs);

        // 2. Mock User repository
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // 3. Mock Cuento repository
        Cuento mockCuento = new Cuento();
        mockCuento.setId(1L);
        mockCuento.setPrecio(10.0); // Set a price for the cuento
        when(cuentoRepository.findById(1L)).thenReturn(Optional.of(mockCuento));


        // 4. Prepare the Order object to be returned by orderRepository.save()
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setTipoDePago("Credit Card"); // This is what we expect to be set

        // 5. When orderRepository.save(any(Order.class)) is called, make it return the savedOrder
        // and capture the argument.
        ArgumentCaptor<Order> orderArgumentCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(orderArgumentCaptor.capture())).thenReturn(savedOrder);

        // 6. Call orderService.save(pedidoDTO)
        Order resultOrder = orderService.save(pedidoDTO);

        // 7. Assert that the tipoDePago on the captured Order object matches the value set in PedidoDTO
        assertEquals("Credit Card", orderArgumentCaptor.getValue().getTipoDePago());
        // Also assert the returned object, though capturing is more robust for checking inputs to save
        assertEquals("Credit Card", resultOrder.getTipoDePago());

        // Verify that save was called
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}

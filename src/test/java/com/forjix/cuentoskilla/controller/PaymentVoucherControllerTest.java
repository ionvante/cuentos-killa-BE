package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.JwtUtil;
import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.config.UserDetailsServiceImpl;
import com.forjix.cuentoskilla.model.Order;
import com.forjix.cuentoskilla.model.OrderStatus;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.Rol;
import com.forjix.cuentoskilla.service.MercadoPagoService;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.StorageService;
import com.forjix.cuentoskilla.service.UserService;
import com.forjix.cuentoskilla.service.PaymentVoucherService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(OrderController.class)
public class PaymentVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService service;

    @MockBean
    private StorageService storageService;

    @MockBean
    private PaymentVoucherService voucherService;

    @MockBean
    private MercadoPagoService mercadoPagoService;

    @MockBean
    private UserService servUser;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    public void uploadVoucherForVerifiedOrderReturnsBadRequest() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@example.com");
        mockUser.setRole(Rol.USER);
        UserDetailsImpl userDetails = new UserDetailsImpl(mockUser);

        Order order = new Order();
        order.setId(1L);
        order.setEstado(OrderStatus.VERIFICADO);
        order.setUser(mockUser);

        Mockito.when(jwtUtil.validateToken(Mockito.anyString())).thenReturn(true);
        Mockito.when(jwtUtil.extractUsername(Mockito.anyString())).thenReturn("user@example.com");
        Mockito.when(userDetailsService.loadUserByUsername(Mockito.anyString())).thenReturn(userDetails);

        Mockito.when(service.getOrderByIdAndUser(1L, 1L)).thenReturn(order);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, "data".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/orders/1/voucher")
                        .file(file)
                        .param("dispositivo", "test")
                        .header("Authorization", "Bearer token"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Order already verified."));
    }
}

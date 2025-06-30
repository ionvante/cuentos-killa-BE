package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.model.DTOs.PedidoDTO;
import com.forjix.cuentoskilla.service.OrderService;
import com.forjix.cuentoskilla.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    private final OrderService orderService;
    private final UserService userService;

    public UserController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('USER')")
    public List<PedidoDTO> getMisPedidos(@RequestParam Long uid) {
        return orderService.getOrders(uid);
    }

    @GetMapping("/perfil")
    @PreAuthorize("hasRole('USER')")
    public User getPerfil(@RequestParam Long uid) {
        return userService.findById(uid).orElse(null);
    }
}

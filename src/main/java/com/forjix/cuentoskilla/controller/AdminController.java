package com.forjix.cuentoskilla.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('ADMIN')")
    public String getPedidosAdmin() {
        System.out.println("getPedidosAdmin() ejecutado");
        return "Listado de pedidos para el administrador";
    }

    @PostMapping("/verificar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String verificarPedido(@PathVariable Long id) {
        return "Pedido " + id + " verificado por el admin";
    }

    @PostMapping("/empaquetar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String empaquetarPedido(@PathVariable Long id) {
        return "Pedido " + id + " empaquetado";
    }

    @PostMapping("/enviar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String enviarPedido(@PathVariable Long id) {
        return "Pedido " + id + " enviado";
    }

    @PostMapping("/entregar/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String entregarPedido(@PathVariable Long id) {
        return "Pedido " + id + " entregado";
    }
}

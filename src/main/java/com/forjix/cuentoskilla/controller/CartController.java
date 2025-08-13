package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.CartItem;
import com.forjix.cuentoskilla.service.CartService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin
public class CartController {
    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<CartItem> getCart(@AuthenticationPrincipal UserDetailsImpl user) {
        return service.getItems(user.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public CartItem add(@RequestBody CartItem item) {
        return service.save(item);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public void remove(@PathVariable Long id) {
        service.delete(id);
    }
}

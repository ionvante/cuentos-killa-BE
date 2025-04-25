package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.CartItem;
import com.forjix.cuentoskilla.model.User;
import com.forjix.cuentoskilla.service.CartService;
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
    public List<CartItem> getCart(User user) {
        return service.getItems(user);
    }

    @PostMapping
    public CartItem add(@RequestBody CartItem item) {
        return service.save(item);
    }

    @DeleteMapping("/{id}")
    public void remove(@PathVariable Long id) {
        service.delete(id);
    }
}

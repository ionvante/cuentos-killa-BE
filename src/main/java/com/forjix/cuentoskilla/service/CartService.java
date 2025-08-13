package com.forjix.cuentoskilla.service;

import com.forjix.cuentoskilla.model.CartItem;
import com.forjix.cuentoskilla.repository.CartItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private final CartItemRepository cartRepo;

    public CartService(CartItemRepository cartRepo) {
        this.cartRepo = cartRepo;
    }

    public List<CartItem> getItems(Long userId) {
        return cartRepo.findByUser_Id(userId);
    }

    public CartItem save(CartItem item) {
        return cartRepo.save(item);
    }

    public void delete(Long id) {
        cartRepo.deleteById(id);
    }
}

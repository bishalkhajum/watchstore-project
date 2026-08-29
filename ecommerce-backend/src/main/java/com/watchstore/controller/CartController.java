package com.watchstore.controller;

import com.watchstore.dto.CartItemRequest;
import com.watchstore.dto.CartItemResponse;
import com.watchstore.model.User;
import com.watchstore.security.CurrentUserResolver;
import com.watchstore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping
    public List<CartItemResponse> getCart(Authentication auth) {
        return cartService.getCart(currentUserResolver.resolve(auth));
    }

    @PostMapping("/items")
    public List<CartItemResponse> addOrUpdate(Authentication auth, @Valid @RequestBody CartItemRequest req) {
        return cartService.addOrUpdate(currentUserResolver.resolve(auth), req);
    }

    @DeleteMapping("/items/{cartItemId}")
    public List<CartItemResponse> remove(Authentication auth, @PathVariable Long cartItemId) {
        return cartService.removeItem(currentUserResolver.resolve(auth), cartItemId);
    }
}

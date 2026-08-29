package com.watchstore.controller;

import com.watchstore.dto.OrderResponse;
import com.watchstore.dto.ProductRequest;
import com.watchstore.dto.ProductResponse;
import com.watchstore.service.OrderService;
import com.watchstore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Everything here is already gated to ROLE_ADMIN in SecurityConfig
// (requestMatchers("/api/admin/**").hasRole("ADMIN")).
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;

    @PostMapping("/products")
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/products/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deactivate(id);
    }

    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderService.getAllOrders();
    }
}

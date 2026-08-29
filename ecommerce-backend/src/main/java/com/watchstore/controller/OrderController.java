package com.watchstore.controller;

import com.watchstore.dto.CheckoutRequest;
import com.watchstore.dto.CheckoutResponse;
import com.watchstore.dto.OrderResponse;
import com.watchstore.model.User;
import com.watchstore.security.CurrentUserResolver;
import com.watchstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/checkout")
    public CheckoutResponse checkout(Authentication auth, @Valid @RequestBody CheckoutRequest req) {
        User user = currentUserResolver.resolve(auth);
        return orderService.checkout(user, req);
    }

    @PostMapping("/{orderNumber}/retry-payment")
    public CheckoutResponse retryPayment(Authentication auth, @PathVariable String orderNumber) {
        User user = currentUserResolver.resolve(auth);
        return orderService.retryPayment(user, orderNumber);
    }

    @GetMapping
    public List<OrderResponse> myOrders(Authentication auth) {
        return orderService.getOrdersForUser(currentUserResolver.resolve(auth));
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse getOne(Authentication auth, @PathVariable String orderNumber) {
        return orderService.getOrder(currentUserResolver.resolve(auth), orderNumber);
    }
}

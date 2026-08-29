package com.watchstore.service;

import com.watchstore.dto.*;
import com.watchstore.exception.ApiException;
import com.watchstore.model.*;
import com.watchstore.repository.CartItemRepository;
import com.watchstore.repository.OrderRepository;
import com.watchstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    /**
     * Turns the user's cart into an Order and kicks off the first eSewa
     * payment attempt. Stock is decremented (reserved) right here at order
     * creation, inside the same transaction as the order - this keeps the
     * "is it in stock" check and the "take it off the shelf" action atomic,
     * so two customers can't both buy the last watch. If payment then fails
     * or times out, the reconciliation job (or a manual cancel) restocks it.
     */
    @Transactional
    public CheckoutResponse checkout(User user, CheckoutRequest req) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new ApiException("Your cart is empty", HttpStatus.BAD_REQUEST);
        }

        Order order = Order.builder()
                .orderNumber("WS-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase())
                .user(user)
                .shippingAddress(req.getShippingAddress())
                .shippingPhone(req.getShippingPhone())
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            Product product = productRepository.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));

            if (ci.getQuantity() > product.getStockQuantity()) {
                throw new ApiException(
                        "\"" + product.getName() + "\" only has " + product.getStockQuantity() + " left in stock",
                        HttpStatus.BAD_REQUEST);
            }

            product.setStockQuantity(product.getStockQuantity() - ci.getQuantity());
            productRepository.save(product);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(ci.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
            order.getItems().add(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);
        cartItemRepository.deleteByUser(user);

        Payment payment = paymentService.createPaymentAttempt(order);
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .esewaPaymentUrl(paymentService.getEsewaPaymentUrl())
                .esewaFormFields(paymentService.buildFormFieldsFor(payment))
                .build();
    }

    /** Re-attempt payment for an order stuck as PENDING_PAYMENT or FAILED (e.g. user abandoned eSewa last time). */
    @Transactional
    public CheckoutResponse retryPayment(User user, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException("Not your order", HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new ApiException("This order is already paid", HttpStatus.BAD_REQUEST);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ApiException("This order was cancelled", HttpStatus.BAD_REQUEST);
        }

        Payment payment = paymentService.createPaymentAttempt(order);
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .esewaPaymentUrl(paymentService.getEsewaPaymentUrl())
                .esewaFormFields(paymentService.buildFormFieldsFor(payment))
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(User user, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ApiException("Not your order", HttpStatus.FORBIDDEN);
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .productId(i.getProduct().getId())
                        .productName(i.getProduct().getName())
                        .quantity(i.getQuantity())
                        .priceAtPurchase(i.getPriceAtPurchase())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(items)
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .build();
    }
}

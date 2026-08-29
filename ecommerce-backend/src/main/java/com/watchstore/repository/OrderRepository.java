package com.watchstore.repository;

import com.watchstore.model.Order;
import com.watchstore.model.OrderStatus;
import com.watchstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
    List<Order> findAllByOrderByCreatedAtDesc();
}

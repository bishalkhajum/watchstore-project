package com.watchstore.repository;

import com.watchstore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueAndCategoryId(Long categoryId);
    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);
}

package com.watchstore.service;

import com.watchstore.dto.ProductRequest;
import com.watchstore.dto.ProductResponse;
import com.watchstore.exception.ApiException;
import com.watchstore.model.Category;
import com.watchstore.model.Product;
import com.watchstore.repository.CategoryRepository;
import com.watchstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> listActive(Long categoryId, String search) {
        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productRepository.findByActiveTrueAndNameContainingIgnoreCase(search.trim());
        } else if (categoryId != null) {
            products = productRepository.findByActiveTrueAndCategoryId(categoryId);
        } else {
            products = productRepository.findByActiveTrue();
        }
        return products.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
        return toResponse(p);
    }

    // --- admin operations ---

    @Transactional
    public ProductResponse create(ProductRequest req) {
        Category category = req.getCategoryId() != null
                ? categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ApiException("Category not found", HttpStatus.BAD_REQUEST))
                : null;

        Product product = Product.builder()
                .name(req.getName())
                .brand(req.getBrand())
                .description(req.getDescription())
                .price(req.getPrice())
                .stockQuantity(req.getStockQuantity())
                .imageUrl(req.getImageUrl())
                .category(category)
                .active(true)
                .build();
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));

        Category category = req.getCategoryId() != null
                ? categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ApiException("Category not found", HttpStatus.BAD_REQUEST))
                : null;

        product.setName(req.getName());
        product.setBrand(req.getBrand());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setImageUrl(req.getImageUrl());
        product.setCategory(category);
        productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public void deactivate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
        product.setActive(false); // soft delete - keeps history for past orders
        productRepository.save(product);
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .brand(p.getBrand())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .build();
    }
}

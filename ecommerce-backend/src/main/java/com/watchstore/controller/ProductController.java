package com.watchstore.controller;

import com.watchstore.dto.ProductResponse;
import com.watchstore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) String search) {
        return productService.listActive(categoryId, search);
    }

    @GetMapping("/{id}")
    public ProductResponse getOne(@PathVariable Long id) {
        return productService.getById(id);
    }
}

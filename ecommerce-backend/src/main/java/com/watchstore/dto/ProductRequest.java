package com.watchstore.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

// used by admin endpoints to create/update a product
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductRequest {
    @NotBlank private String name;
    private String brand;
    private String description;
    @NotNull @DecimalMin("0.0") private BigDecimal price;
    @NotNull @Min(0) private Integer stockQuantity;
    private String imageUrl;
    private Long categoryId;
}

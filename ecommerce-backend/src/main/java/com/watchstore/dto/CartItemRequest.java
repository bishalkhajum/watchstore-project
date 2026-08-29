package com.watchstore.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CartItemRequest {
    @NotNull private Long productId;
    @NotNull @Min(1) private Integer quantity;
}

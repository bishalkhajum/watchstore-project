package com.watchstore.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CheckoutRequest {
    @NotBlank private String shippingAddress;
    @NotBlank private String shippingPhone;
}

package com.watchstore.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentVerifyResponse {
    private String orderNumber;
    private String orderStatus;   // OrderStatus as string
    private String paymentStatus; // PaymentStatus as string
    private String message;
}

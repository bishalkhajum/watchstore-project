package com.watchstore.model;

public enum OrderStatus {
    PENDING_PAYMENT, // order created, waiting for payment to complete
    PAID,            // payment confirmed, ready to fulfill
    FAILED,           // payment failed or was cancelled
    CANCELLED         // cancelled by user/admin before payment
}

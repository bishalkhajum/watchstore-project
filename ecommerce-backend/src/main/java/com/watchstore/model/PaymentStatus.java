package com.watchstore.model;

public enum PaymentStatus {
    INITIATED,  // we generated the eSewa form and sent user off to pay
    SUCCESS,    // verified with eSewa as complete
    FAILED,     // verified with eSewa as failed, or verification failed
    PENDING     // sent to gateway, not yet confirmed (used by reconciliation job)
}

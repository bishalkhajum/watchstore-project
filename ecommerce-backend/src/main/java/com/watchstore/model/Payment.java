package com.watchstore.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// One row per payment ATTEMPT. An order can have several (e.g. first attempt
// failed, user retries) - this is what lets us tell "never paid" apart from
// "paid on the second try" without losing history.
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // What we send to eSewa as transaction_uuid for THIS attempt.
    // Format: <orderNumber>-<attemptNumber>, guaranteed unique per attempt.
    @Column(nullable = false, unique = true)
    private String transactionUuid;

    // eSewa's own reference id, returned only on success. Useful for support
    // tickets / manual reconciliation with eSewa's merchant dashboard.
    private String esewaRefId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    // Raw JSON from eSewa's status-check API, kept for debugging/audit trail.
    @Column(length = 2000)
    private String gatewayResponseRaw;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime verifiedAt;
}

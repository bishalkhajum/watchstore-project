package com.watchstore.service;

import com.watchstore.dto.PaymentVerifyResponse;
import com.watchstore.exception.ApiException;
import com.watchstore.model.*;
import com.watchstore.repository.OrderRepository;
import com.watchstore.repository.PaymentRepository;
import com.watchstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Owns the payment lifecycle. Two ways a Payment gets confirmed:
 *
 *  1) verifyByTransactionUuid() - called synchronously when the browser
 *     comes back from eSewa (Approach A from the design doc). Fast path,
 *     covers the vast majority of real users.
 *
 *  2) PaymentReconciliationJob - a scheduled sweep that catches orders left
 *     PENDING_PAYMENT too long (user closed the tab, phone died, etc.) and
 *     re-checks them the same way. This is the safety net Approach A alone
 *     is missing.
 *
 * Both paths funnel through checkAndUpdate() so the update logic - and the
 * "never downgrade a PAID order" guard - lives in exactly one place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository; // for restocking on confirmed payment failure
    private final EsewaService esewaService;

    public String getEsewaPaymentUrl() {
        return esewaService.getPaymentUrl();
    }

    @Transactional
    public Payment createPaymentAttempt(Order order) {
        long attemptCount = paymentRepository.findByOrderId(order.getId()).size();
        String transactionUuid = order.getOrderNumber() + "-" + (attemptCount + 1);

        Payment payment = Payment.builder()
                .order(order)
                .transactionUuid(transactionUuid)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.INITIATED)
                .build();
        return paymentRepository.save(payment);
    }

    public Map<String, String> buildFormFieldsFor(Payment payment) {
        return esewaService.buildPaymentFormFields(payment.getTransactionUuid(), payment.getAmount());
    }

    /**
     * Called from the callback endpoint the frontend hits right after eSewa
     * redirects the user back. Independently re-verifies with eSewa - the
     * redirect itself is untrusted input, it just tells us WHICH transaction
     * to go check.
     */
    @Transactional
    public PaymentVerifyResponse verifyByTransactionUuid(String transactionUuid) {
        Payment payment = paymentRepository.findByTransactionUuid(transactionUuid)
                .orElseThrow(() -> new ApiException("Unknown payment reference", HttpStatus.NOT_FOUND));

        checkAndUpdate(payment);

        Order order = payment.getOrder();
        return PaymentVerifyResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus().name())
                .paymentStatus(payment.getStatus().name())
                .message(messageFor(payment.getStatus()))
                .build();
    }

    /**
     * The actual verify-and-apply logic, shared by the redirect callback and
     * the reconciliation job. Idempotent: calling this twice on an already
     * SUCCESS payment is a no-op, so double callbacks / overlapping job runs
     * can't double-process an order.
     */
    @Transactional
    public void checkAndUpdate(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return; // already confirmed - idempotent, do nothing
        }

        EsewaService.EsewaStatusResult result =
                esewaService.checkTransactionStatus(payment.getTransactionUuid(), payment.getAmount());

        payment.setGatewayResponseRaw(result.rawResponse());

        if (result.isComplete()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setEsewaRefId(result.refId());
            payment.setVerifiedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            // Guard: never flip an already-PAID order, and never mark PAID
            // an order that was already restocked as FAILED/CANCELLED - if
            // that ever happens it needs a human, not an auto-overwrite.
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.PAID);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
            } else if (order.getStatus() != OrderStatus.PAID) {
                log.warn("Payment {} verified SUCCESS but order {} was already {} - needs manual review",
                        payment.getTransactionUuid(), order.getOrderNumber(), order.getStatus());
            }

        } else if (result.isDefinitelyFailed()) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            restockAndMarkFailed(payment.getOrder());

        } else {
            // PENDING / AMBIGUOUS / CHECK_ERROR - genuinely unknown right
            // now. Leave it as PENDING so the reconciliation job checks
            // again later, rather than guessing.
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
        }
    }

    /**
     * Restocks the order's items and marks it FAILED. Lives here (not in
     * OrderService) specifically to avoid a circular Spring bean dependency:
     * OrderService already depends on PaymentService to create payment
     * attempts, so PaymentService can't also depend on OrderService.
     */
    private void restockAndMarkFailed(Order order) {
        if (order.getStatus() == OrderStatus.PAID) return; // never undo a completed sale
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }
        order.setStatus(OrderStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    /** Used by the scheduled reconciliation job. */
    public List<Payment> findStalePendingPayments(LocalDateTime cutoff) {
        List<Payment> initiated = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.INITIATED, cutoff);
        List<Payment> pending = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);
        initiated.addAll(pending);
        return initiated;
    }

    private String messageFor(PaymentStatus status) {
        return switch (status) {
            case SUCCESS -> "Payment confirmed. Thank you for your order!";
            case FAILED -> "Payment failed or was cancelled. You can try again from your order history.";
            case PENDING -> "We're still confirming your payment with eSewa. This can take a few minutes - check your order history shortly.";
            case INITIATED -> "Payment not yet completed.";
        };
    }
}

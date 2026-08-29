package com.watchstore.scheduler;

import com.watchstore.model.Payment;
import com.watchstore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The safety net described in the design doc: catches payments where the
 * user never came back to trigger the redirect-callback verification (tab
 * closed, phone died, network dropped mid-redirect). Every few minutes, find
 * payment attempts that have been sitting unresolved for more than a couple
 * minutes and independently re-check them with eSewa.
 *
 * This is what makes "verify on redirect" (Approach A) safe to use without a
 * queue or reliable webhooks: nothing depends on the user's browser
 * cooperating, it just might take a few extra minutes to notice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final PaymentService paymentService;

    private static final int STALE_AFTER_MINUTES = 3;

    @Scheduled(fixedDelay = 2 * 60 * 1000) // every 2 minutes
    public void reconcile() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STALE_AFTER_MINUTES);
        List<Payment> stale = paymentService.findStalePendingPayments(cutoff);

        if (stale.isEmpty()) return;

        log.info("Reconciliation: checking {} stale payment attempt(s)", stale.size());
        for (Payment payment : stale) {
            try {
                paymentService.checkAndUpdate(payment);
            } catch (Exception e) {
                // one bad attempt shouldn't stop the rest of the sweep
                log.error("Reconciliation failed for payment {}", payment.getTransactionUuid(), e);
            }
        }
    }
}

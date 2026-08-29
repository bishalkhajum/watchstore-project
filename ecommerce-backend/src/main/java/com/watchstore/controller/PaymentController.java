package com.watchstore.controller;

import com.watchstore.dto.PaymentVerifyResponse;
import com.watchstore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Hit by the frontend's PaymentResult page right after the eSewa
     * redirect (whether eSewa says success or failure - we re-check either
     * way, since the redirect itself is untrusted). Public endpoint: no auth
     * required, since the browser lands here straight from eSewa. The
     * transaction_uuid is unguessable enough for this purpose and the only
     * thing it can do is trigger a read-only re-check against eSewa itself.
     */
    @GetMapping("/verify/{transactionUuid}")
    public PaymentVerifyResponse verify(@PathVariable String transactionUuid) {
        return paymentService.verifyByTransactionUuid(transactionUuid);
    }
}

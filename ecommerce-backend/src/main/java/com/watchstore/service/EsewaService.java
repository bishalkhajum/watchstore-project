package com.watchstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Talks to eSewa's ePay v2 API (sandbox / RC environment).
 *
 * eSewa's flow is NOT a simple REST call for payment itself - the browser
 * must be redirected to eSewa via an HTML form POST (we build the fields +
 * an HMAC-SHA256 signature here, the frontend just renders and auto-submits
 * the form). After the user pays, eSewa redirects the browser back to our
 * success/failure URL - but per the design discussion, we NEVER trust that
 * redirect alone. We always independently call eSewa's status-check API
 * with the transaction_uuid to confirm before marking anything as paid.
 */
@Slf4j
@Service
public class EsewaService {

    private final String merchantCode;
    private final String secretKey;
    private final String paymentUrl;
    private final String statusCheckUrl;
    private final String successUrl;
    private final String failureUrl;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EsewaService(@Value("${esewa.merchant-code}") String merchantCode,
                         @Value("${esewa.secret-key}") String secretKey,
                         @Value("${esewa.payment-url}") String paymentUrl,
                         @Value("${esewa.status-check-url}") String statusCheckUrl,
                         @Value("${esewa.success-url}") String successUrl,
                         @Value("${esewa.failure-url}") String failureUrl) {
        this.merchantCode = merchantCode;
        this.secretKey = secretKey;
        this.paymentUrl = paymentUrl;
        this.statusCheckUrl = statusCheckUrl;
        this.successUrl = successUrl;
        this.failureUrl = failureUrl;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    /**
     * Builds the full set of form fields the frontend must POST to eSewa,
     * including the HMAC signature. transactionUuid must be unique PER
     * PAYMENT ATTEMPT (not just per order) - see Payment.transactionUuid.
     */
    public Map<String, String> buildPaymentFormFields(String transactionUuid, BigDecimal totalAmount) {
        String amountStr = totalAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("amount", amountStr);
        fields.put("tax_amount", "0");
        fields.put("total_amount", amountStr);
        fields.put("transaction_uuid", transactionUuid);
        fields.put("product_code", merchantCode);
        fields.put("product_service_charge", "0");
        fields.put("product_delivery_charge", "0");
        // success_url/failure_url are NOT in signed_field_names, so it's
        // safe to append the transaction reference here - the frontend reads
        // it back off the URL to know which payment to independently verify.
        String sep = successUrl.contains("?") ? "&" : "?";
        fields.put("success_url", successUrl + sep + "transaction_uuid=" + transactionUuid);
        fields.put("failure_url", failureUrl + sep + "transaction_uuid=" + transactionUuid);
        fields.put("signed_field_names", "total_amount,transaction_uuid,product_code");

        String signature = sign(fields, "total_amount,transaction_uuid,product_code");
        fields.put("signature", signature);

        return fields;
    }

    /**
     * HMAC-SHA256 over "key=value,key=value,..." for the signed fields, in
     * the exact order given, base64-encoded. This is eSewa's required
     * signing scheme - the order of fields in signedFieldNames matters.
     */
    private String sign(Map<String, String> fields, String signedFieldNames) {
        try {
            StringBuilder message = new StringBuilder();
            String[] names = signedFieldNames.split(",");
            for (int i = 0; i < names.length; i++) {
                if (i > 0) message.append(",");
                message.append(names[i]).append("=").append(fields.get(names[i]));
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign eSewa payment request", e);
        }
    }

    /**
     * Independently asks eSewa "did this transaction actually complete?"
     * This is the call that matters - never trust the browser redirect by
     * itself. Returns eSewa's raw status string, e.g.:
     * COMPLETE | PENDING | FULL_REFUND | PARTIAL_REFUND | AMBIGUOUS | NOT_FOUND | CANCELED
     */
    public EsewaStatusResult checkTransactionStatus(String transactionUuid, BigDecimal totalAmount) {
        String amountStr = totalAmount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        try {
            String rawResponse = restClient.get()
                    .uri(statusCheckUrl + "?product_code={pc}&total_amount={amt}&transaction_uuid={uuid}",
                            merchantCode, amountStr, transactionUuid)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(rawResponse);
            String status = json.path("status").asText("NOT_FOUND");
            String refId = json.has("ref_id") ? json.path("ref_id").asText(null) : null;

            return new EsewaStatusResult(status, refId, rawResponse);
        } catch (Exception e) {
            log.error("eSewa status check failed for transaction {}", transactionUuid, e);
            // Network/parse failure is NOT the same as "payment failed" - we
            // report it distinctly so the caller keeps the payment PENDING
            // and lets the reconciliation job retry later, instead of
            // wrongly marking a possibly-successful payment as failed.
            return new EsewaStatusResult("CHECK_ERROR", null, e.getMessage());
        }
    }

    public record EsewaStatusResult(String status, String refId, String rawResponse) {
        public boolean isComplete() {
            return "COMPLETE".equalsIgnoreCase(status);
        }
        public boolean isDefinitelyFailed() {
            return "CANCELED".equalsIgnoreCase(status) || "NOT_FOUND".equalsIgnoreCase(status);
        }
    }
}

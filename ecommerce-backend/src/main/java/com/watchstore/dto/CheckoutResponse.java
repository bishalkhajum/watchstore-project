package com.watchstore.dto;

import lombok.*;
import java.util.Map;

// Everything the frontend needs to auto-submit the eSewa payment form.
// eSewa's flow requires an actual HTML form POST (not a JSON API call) with
// an HMAC signature, so the backend builds all the fields + signature and
// the frontend just renders + auto-submits a <form>.
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheckoutResponse {
    private Long orderId;
    private String orderNumber;
    private String esewaPaymentUrl;
    private Map<String, String> esewaFormFields;
}

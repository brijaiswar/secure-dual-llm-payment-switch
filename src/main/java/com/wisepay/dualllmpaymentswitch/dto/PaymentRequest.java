package com.wisepay.dualllmpaymentswitch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotBlank(message = "Prompt parameter is required")
        @Size(max = 2_000, message = "Prompt exceeds maximum length")
        String prompt,

        @Size(max = 32, message = "Source exceeds maximum length")
        String source,

        boolean confirmed,

        @Size(max = 64, message = "Intent hash exceeds maximum length")
        String intentHash
) {
}

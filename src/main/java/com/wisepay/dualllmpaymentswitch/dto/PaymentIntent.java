package com.wisepay.dualllmpaymentswitch.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentIntent(
        @NotBlank(message = "Recipient VPA is required")
        @Pattern(
                regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,64}$",
                message = "Invalid VPA format. Expected user@bank"
        )
        String recipientVpa,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @DecimalMax(value = "100000.00", message = "Amount exceeds single-transaction limit of 1,00,000")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^(INR|USD|EUR)$", message = "Unsupported currency code")
        String currency,

        @Size(max = 100, message = "Remarks string exceeds maximum length")
        String remarks
) {}

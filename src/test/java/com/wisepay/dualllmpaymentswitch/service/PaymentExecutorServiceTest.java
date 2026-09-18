package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentExecutorServiceTest {

    @Test
    void returnsCoreTransactionIdentifier() {
        String transactionId = new PaymentExecutorService().executeTransaction(
                new PaymentIntent("power@grid", new BigDecimal("500"), "INR", null),
                "payment-client");

        assertTrue(transactionId.startsWith("TXN_"));
    }
}

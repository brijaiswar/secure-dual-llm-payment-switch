package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IntentHashServiceTest {

    private final IntentHashService hashes = new IntentHashService();

    @Test
    void isStableAndCanonicalForEquivalentAmounts() {
        var first = new PaymentIntent("power@grid", new BigDecimal("500.00"), "INR", null);
        var second = new PaymentIntent("power@grid", new BigDecimal("500"), "INR", null);

        assertEquals(hashes.hash(first), hashes.hash(second));
        assertEquals(64, hashes.hash(first).length());
    }

    @Test
    void changesWhenPaymentFieldsChange() {
        var first = new PaymentIntent("power@grid", new BigDecimal("500"), "INR", null);
        var changed = new PaymentIntent("attacker@paytm", new BigDecimal("500"), "INR", null);

        assertNotEquals(hashes.hash(first), hashes.hash(changed));
    }
}

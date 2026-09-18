package com.wisepay.dualllmpaymentswitch.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlannerAiServiceTest {

    private final PlannerAiService planner = new PlannerAiService();

    @Test
    void parsesIndianCommaAmountAndCurrency() {
        var intent = planner.parseIntent("Pay INR 1,00,000 to power@grid");

        assertEquals("power@grid", intent.recipientVpa());
        assertEquals(new BigDecimal("100000"), intent.amount());
        assertEquals("INR", intent.currency());
    }

    @Test
    void parsesUsdAndEurCurrency() {
        assertEquals("USD", planner.parseIntent("Send USD 10 to user@bank").currency());
        assertEquals("EUR", planner.parseIntent("Send EUR 10 to user@bank").currency());
    }

    @Test
    void returnsMissingFieldsForUnstructuredText() {
        var intent = planner.parseIntent(null);

        assertNull(intent.recipientVpa());
        assertNull(intent.amount());
        assertEquals("INR", intent.currency());
    }

    @Test
    void doesNotTreatDigitsInRecipientAsAmount() {
        var intent = planner.parseIntent("Pay user123@bank");

        assertEquals("user123@bank", intent.recipientVpa());
        assertNull(intent.amount());
    }
}

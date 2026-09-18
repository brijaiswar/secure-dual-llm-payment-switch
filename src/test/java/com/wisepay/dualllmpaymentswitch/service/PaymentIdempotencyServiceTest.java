package com.wisepay.dualllmpaymentswitch.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentIdempotencyServiceTest {

    @Test
    void executesOperationOncePerSubjectAndKey() {
        var service = new PaymentIdempotencyService();
        var calls = new AtomicInteger();

        String first = service.executeOnce("alice", "payment-001", "intent-a",
                () -> "TXN-" + calls.incrementAndGet());
        String second = service.executeOnce("alice", "payment-001", "intent-a",
                () -> "TXN-" + calls.incrementAndGet());
        String differentSubject = service.executeOnce("bob", "payment-001", "intent-a",
                () -> "TXN-" + calls.incrementAndGet());

        assertEquals("TXN-1", first);
        assertEquals(first, second);
        assertEquals("TXN-2", differentSubject);
        assertEquals(2, calls.get());
    }

    @Test
    void rejectsReuseForDifferentIntent() {
        var service = new PaymentIdempotencyService();
        service.executeOnce("alice", "payment-002", "intent-a", () -> "TXN-1");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.executeOnce("alice", "payment-002", "intent-b", () -> "TXN-2"));
    }
}

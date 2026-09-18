package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class PaymentAuditServiceTest {

    @Test
    void handlesNullAndUnsafeSourceValues() {
        var audit = new PaymentAuditService();
        var intent = new PaymentIntent("power@grid", new BigDecimal("500"), "INR", null);

        audit.blocked("subject", null, "blocked");
        audit.proposed("subject", "invoice/email", intent);
        audit.executed("subject", "invoice/email", "TXN_1");
    }
}

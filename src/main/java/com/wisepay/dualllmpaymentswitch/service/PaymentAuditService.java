package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentAuditService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuditService.class);

    public void blocked(String subject, String source, String reason) {
        log.warn("PAYMENT_BLOCKED subject={} source={} reason={}", subject, safe(source), reason);
    }

    public void proposed(String subject, String source, PaymentIntent intent) {
        log.info("PAYMENT_PROPOSED subject={} source={} recipient={} amount={} currency={}",
                subject, safe(source), intent.recipientVpa(), intent.amount(), intent.currency());
    }

    public void executed(String subject, String source, String transactionId) {
        log.info("PAYMENT_EXECUTED subject={} source={} transactionId={}",
                subject, safe(source), transactionId);
    }

    private String safe(String source) {
        return source == null || source.isBlank() ? "unknown" : source.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

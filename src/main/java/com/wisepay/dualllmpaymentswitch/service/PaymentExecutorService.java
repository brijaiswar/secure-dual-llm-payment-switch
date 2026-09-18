package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentExecutorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExecutorService.class);

    public String executeTransaction(PaymentIntent intent, String authenticatedSubject) {
        // Authentication and authorization are supplied by the caller, never by model output.
        log.info("Executing payment via Core Banking Adapter for subject {}: recipient={}, amount={} {}",
                authenticatedSubject, intent.recipientVpa(), intent.amount(), intent.currency());

        // The adapter boundary is where an idempotency key and bank transaction are bound.
        return "TXN_" + UUID.randomUUID();
    }
}
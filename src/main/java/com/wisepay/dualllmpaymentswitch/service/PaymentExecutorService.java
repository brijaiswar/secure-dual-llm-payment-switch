package com.wisepay.dualllmpaymentswitch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentExecutorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentExecutorService.class);

    public String executeTransaction(PaymentIntent intent, String userAuthHeader) {
        // Authenticate context using user security token, NOT model output
        log.info("Executing payment via Core Banking Adapter...");
        log.info("Recipient: {}, Amount: {} {}, AuthToken: {}",
                intent.recipientVpa(), intent.amount(), intent.currency(), userAuthHeader.substring(0, 12) + "...");

        // Simulated integration with ISO 20022/UPI Switch
        return "TXN_" + System.currentTimeMillis();
    }
}
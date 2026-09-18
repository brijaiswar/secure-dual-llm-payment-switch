package com.wisepay.dualllmpaymentswitch.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class PaymentIdempotencyService {

    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private final Map<String, String> completedTransactions = new ConcurrentHashMap<>();
    private final Map<String, String> requestFingerprints = new ConcurrentHashMap<>();

    public String executeOnce(String subject, String idempotencyKey, String requestFingerprint,
                              Supplier<String> operation) {
        String scopedKey = subject + ":" + idempotencyKey;
        Object lock = locks.computeIfAbsent(scopedKey, ignored -> new Object());
        synchronized (lock) {
            String existingFingerprint = requestFingerprints.get(scopedKey);
            if (existingFingerprint != null && !existingFingerprint.equals(requestFingerprint)) {
                throw new IllegalStateException("Idempotency key was reused for a different payment intent");
            }
            String existing = completedTransactions.get(scopedKey);
            if (existing != null) {
                return existing;
            }
            String transactionId = operation.get();
            requestFingerprints.put(scopedKey, requestFingerprint);
            completedTransactions.put(scopedKey, transactionId);
            return transactionId;
        }
    }
}

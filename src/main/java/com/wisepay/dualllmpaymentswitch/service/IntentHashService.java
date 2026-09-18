package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class IntentHashService {

    public String hash(PaymentIntent intent) {
        String canonical = String.join("|",
                value(intent.recipientVpa()),
                intent.amount() == null ? "" : intent.amount().stripTrailingZeros().toPlainString(),
                value(intent.currency()),
                value(intent.remarks()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}

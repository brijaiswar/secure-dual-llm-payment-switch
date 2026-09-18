package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.math.BigDecimal;

@Service
public class IntentVerifierService {

    private static final Set<String> INJECTION_MARKERS = Set.of(
            "ignore previous instructions", "system prompt", "developer message",
            "bypass", "override policy", "reveal instructions");
    private static final Pattern DIRECTIVE = Pattern.compile(
            "(?i)\\b(?:transfer|send|pay)\\b.*?(₹|rs\\.?|inr|usd|eur)?\\s*([\\d,]+(?:\\.\\d{1,2})?).*?\\bto\\s+([a-z0-9._-]+@[a-z]{2,64})");

    public VerificationResult verify(String rawPrompt, PaymentIntent proposal) {
        if (proposal == null) {
            return VerificationResult.block("Planner returned no intent");
        }
        String prompt = rawPrompt == null ? "" : rawPrompt.toLowerCase(Locale.ROOT);
        List<String> risks = INJECTION_MARKERS.stream()
                .filter(prompt::contains)
                .toList();
        if (!risks.isEmpty()) {
            return VerificationResult.block("Prompt contains command-injection indicators");
        }
        var directives = DIRECTIVE.matcher(rawPrompt == null ? "" : rawPrompt);
        String recipient = null;
        String amount = null;
        String currency = null;
        while (directives.find()) {
            String directiveCurrency = normalizeCurrency(directives.group(1));
            if (recipient != null && (!recipient.equalsIgnoreCase(directives.group(3))
                    || !amount.equals(directives.group(2).replace(",", ""))
                    || !currency.equals(directiveCurrency))) {
                return VerificationResult.block("Prompt contains conflicting payment directives");
            }
            recipient = directives.group(3);
            amount = directives.group(2).replace(",", "");
            currency = directiveCurrency;
        }
        if (recipient != null && (!recipient.equalsIgnoreCase(proposal.recipientVpa())
                || proposal.amount() == null
                || proposal.amount().compareTo(new BigDecimal(amount)) != 0
                || (currency != null && !currency.equals(proposal.currency())))) {
            return VerificationResult.block("Planner proposal does not match the payment directive");
        }
        if (proposal.remarks() != null && proposal.remarks().toLowerCase(Locale.ROOT).contains("ignore")) {
            return VerificationResult.block("Untrusted text was promoted into payment metadata");
        }
        return VerificationResult.allow();
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("₹")
                || value.toLowerCase(Locale.ROOT).startsWith("rs")
                || value.equalsIgnoreCase("inr")) {
            return "INR";
        }
        if (value.equalsIgnoreCase("usd")) {
            return "USD";
        }
        if (value.equalsIgnoreCase("eur")) {
            return "EUR";
        }
        return "UNSUPPORTED";
    }

    public record VerificationResult(boolean allowed, String reason) {
        static VerificationResult allow() {
            return new VerificationResult(true, null);
        }

        static VerificationResult block(String reason) {
            return new VerificationResult(false, reason);
        }
    }
}

package com.wisepay.dualllmpaymentswitch.service;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Planner boundary. A production deployment replaces this implementation with an
 * isolated, unprivileged model adapter that can only return a typed proposal.
 */
@Service
public class PlannerAiService {

    private static final Pattern VPA = Pattern.compile("([a-zA-Z0-9._-]{2,256}@[a-zA-Z]{2,64})");
    private static final Pattern CURRENCY_AMOUNT = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr|usd|eur)\\s*([\\d,]+(?:\\.\\d{1,2})?)");

    public PaymentIntent parseIntent(String prompt) {
        String normalized = prompt == null ? "" : prompt.trim();
        Matcher vpaMatcher = VPA.matcher(normalized);
        String vpa = vpaMatcher.find() ? vpaMatcher.group(1) : null;
        String withoutVpa = VPA.matcher(normalized).replaceAll(" ");
        Matcher amountMatcher = CURRENCY_AMOUNT.matcher(withoutVpa);
        BigDecimal amount = amountMatcher.find()
                ? new BigDecimal(amountMatcher.group(1).replace(",", "")) : null;
        String upper = normalized.toUpperCase(Locale.ROOT);
        String currency = upper.matches(".*\\bUSD\\b.*") ? "USD"
                : upper.matches(".*\\bEUR\\b.*") ? "EUR" : "INR";
        return new PaymentIntent(vpa, amount, currency, null);
    }
}

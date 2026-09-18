package com.wisepay.dualllmpaymentswitch.controller;

import com.wisepay.dualllmpaymentswitch.dto.PaymentIntent;
import com.wisepay.dualllmpaymentswitch.dto.PaymentRequest;
import com.wisepay.dualllmpaymentswitch.service.PaymentAuditService;
import com.wisepay.dualllmpaymentswitch.service.IntentVerifierService;
import com.wisepay.dualllmpaymentswitch.service.PaymentExecutorService;
import com.wisepay.dualllmpaymentswitch.service.PaymentIdempotencyService;
import com.wisepay.dualllmpaymentswitch.service.IntentHashService;
import com.wisepay.dualllmpaymentswitch.service.PlannerAiService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PlannerAiService plannerAiService;
    private final PaymentExecutorService executorService;
    private final PaymentIdempotencyService idempotencyService;
    private final IntentVerifierService verifierService;
    private final PaymentAuditService auditService;
    private final IntentHashService intentHashService;
    private final Validator validator;

    public PaymentController(
            PlannerAiService plannerAiService,
            PaymentExecutorService executorService,
            PaymentIdempotencyService idempotencyService,
            IntentVerifierService verifierService,
            PaymentAuditService auditService,
            IntentHashService intentHashService,
            Validator validator) {
        this.plannerAiService = plannerAiService;
        this.executorService = executorService;
        this.idempotencyService = idempotencyService;
        this.verifierService = verifierService;
        this.auditService = auditService;
        this.intentHashService = intentHashService;
        this.validator = validator;
    }

    @PostMapping("/process-nl")
    public ResponseEntity<?> processNaturalLanguagePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {

        if (idempotencyKey == null || !idempotencyKey.matches("^[A-Za-z0-9._:-]{8,128}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "A valid Idempotency-Key is required"));
        }
        String rawPrompt = request.prompt();
        String source = request.source();
        // STEP 1: The planner proposes data only; it has no tools or payment privileges.
        PaymentIntent rawIntent = plannerAiService.parseIntent(rawPrompt);
        auditService.proposed(authentication.getName(), source, rawIntent);

        // STEP 2: A separate verifier rejects prompt-injection indicators before execution.
        IntentVerifierService.VerificationResult verification =
                verifierService.verify(rawPrompt, rawIntent);
        if (!verification.allowed()) {
            auditService.blocked(authentication.getName(), source, verification.reason());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", "BLOCKED_BY_GUARDRAIL",
                    "reason", verification.reason()
            ));
        }

        // STEP 3: Programmatic schema validation is authoritative over model output.
        Set<ConstraintViolation<PaymentIntent>> violations = validator.validate(rawIntent);
        if (!violations.isEmpty()) {
            auditService.blocked(authentication.getName(), source, "Typed intent failed schema validation");
            Map<String, String> errors = violations.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            v -> v.getPropertyPath().toString(),
                            ConstraintViolation::getMessage,
                            (msg1, msg2) -> msg1
                    ));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "status", "BLOCKED_BY_GUARDRAIL",
                    "validationErrors", errors
            ));
        }

        String computedIntentHash = intentHashService.hash(rawIntent);
        if (!request.confirmed()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "status", "CONFIRMATION_REQUIRED",
                    "message", "Review the validated intent and resubmit with confirmed=true and the returned intentHash",
                    "intentHash", computedIntentHash,
                    "validatedIntent", rawIntent
            ));
        }
        if (request.intentHash() == null
                || !request.intentHash().matches("^[a-fA-F0-9]{64}$")
                || !MessageDigest.isEqual(request.intentHash().toLowerCase().getBytes(StandardCharsets.UTF_8),
                computedIntentHash.getBytes(StandardCharsets.UTF_8))) {
            auditService.blocked(authentication.getName(), source, "Confirmation does not match validated intent");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "CONFIRMATION_REQUIRED",
                    "message", "The confirmed intent does not match the validated intent"
            ));
        }

        // STEP 4: Dispatch only the validated, typed intent to the non-AI core.
        String transactionId;
        try {
            transactionId = idempotencyService.executeOnce(
                    authentication.getName(),
                    idempotencyKey,
                    computedIntentHash,
                    () -> executorService.executeTransaction(rawIntent, authentication.getName()));
        } catch (IllegalStateException exception) {
            auditService.blocked(authentication.getName(), source, "Idempotency key conflict");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "IDEMPOTENCY_KEY_CONFLICT",
                    "message", "The idempotency key was already used for a different payment intent"
            ));
        }
        auditService.executed(authentication.getName(), source, transactionId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", transactionId,
                "idempotencyKey", idempotencyKey,
                "validatedIntent", rawIntent
        ));
    }
}

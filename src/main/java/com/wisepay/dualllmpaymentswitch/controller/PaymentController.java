package com.wisepay.dualllmpaymentswitch.controller;

package com.bank.payment.controller;

import com.bank.payment.dto.PaymentIntent;
import com.bank.payment.service.PaymentExecutorService;
import com.bank.payment.service.PlannerAiService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PlannerAiService plannerAiService;
    private final PaymentExecutorService executorService;
    private final Validator validator;

    public PaymentController(
            PlannerAiService plannerAiService,
            PaymentExecutorService executorService,
            Validator validator) {
        this.plannerAiService = plannerAiService;
        this.executorService = executorService;
        this.validator = validator;
    }

    @PostMapping("/process-nl")
    public ResponseEntity<?> processNaturalLanguagePayment(
            @RequestBody Map<String, String> requestBody,
            @RequestHeader("Authorization") String authHeader) {

        String rawPrompt = requestBody.get("prompt");
        if (rawPrompt == null || rawPrompt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt parameter is required"));
        }

        // STEP 1: Unprivileged Planner LLM extracts structured intent
        PaymentIntent rawIntent = plannerAiService.parseIntent(rawPrompt);

        // STEP 2: Programmatic Bean Validation (Pydantic Equivalent)
        Set<ConstraintViolation<PaymentIntent>> violations = validator.validate(rawIntent);
        if (!violations.isEmpty()) {
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

        // STEP 3: Dispatch to non-AI Core Payment System
        String transactionId = executorService.executeTransaction(rawIntent, authHeader);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", transactionId,
                "validatedIntent", rawIntent
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Processing failed", "details", ex.getMessage()));
    }
}

package com.edumanage.sms.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CreateSmsConfigurationRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void apiKeyIsRequired() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("DARSMS");
        req.setApiKey("");

        Set<ConstraintViolation<CreateSmsConfigurationRequest>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("apiKey")));
    }

    @Test
    void apiSecretIsOptional() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("DARSMS");
        req.setApiKey("valid-key");
        req.setApiSecret(null);

        Set<ConstraintViolation<CreateSmsConfigurationRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "apiSecret must be optional: " + violations);
    }

    @Test
    void providerTooLongIsRejected() {
        CreateSmsConfigurationRequest req = new CreateSmsConfigurationRequest();
        req.setSchoolId(1L);
        req.setProvider("a".repeat(51));
        req.setApiKey("valid-key");

        Set<ConstraintViolation<CreateSmsConfigurationRequest>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("provider")));
    }
}

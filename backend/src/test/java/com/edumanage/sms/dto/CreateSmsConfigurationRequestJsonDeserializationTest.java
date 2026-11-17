package com.edumanage.sms.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateSmsConfigurationRequestJsonDeserializationTest {

    @Test
    void shouldDeserializeValidDarsmsCreateRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = """
                {
                  "schoolId": 1,
                  "provider": "DARSMS",
                  "apiKey": "test-api-key",
                  "senderId": "DARSMS",
                  "isActive": true,
                  "monthlyLimit": 700,
                  "dailyLimit": 45,
                  "remarks": "to all people"
                }
                """;

        CreateSmsConfigurationRequest req = mapper.readValue(json, CreateSmsConfigurationRequest.class);

        assertEquals(Long.valueOf(1), req.getSchoolId());
        assertEquals("DARSMS", req.getProvider());
        assertEquals("test-api-key", req.getApiKey());
        assertNull(req.getApiSecret());
        assertEquals("DARSMS", req.getSenderId());
        assertTrue(req.isActive());
        assertEquals(Integer.valueOf(700), req.getMonthlyLimit());
        assertEquals(Integer.valueOf(45), req.getDailyLimit());
        assertEquals("to all people", req.getRemarks());
    }

    @Test
    void shouldDeserializeWithNullApiSecret() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = """
                {
                  "schoolId": 1,
                  "provider": "DARSMS",
                  "apiKey": "test-api-key",
                  "apiSecret": null,
                  "senderId": "NEWGENESIS",
                  "isActive": true,
                  "monthlyLimit": 700,
                  "dailyLimit": 45,
                  "remarks": "to all people"
                }
                """;

        CreateSmsConfigurationRequest req = mapper.readValue(json, CreateSmsConfigurationRequest.class);

        assertEquals("NEWGENESIS", req.getSenderId());
        assertNull(req.getApiSecret());
    }

    @Test
    void shouldDeserializeWithoutApiSecretField() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // apiSecret field omitted from JSON (as happens when frontend sends undefined)
        String json = """
                {
                  "schoolId": 1,
                  "provider": "DARSMS",
                  "apiKey": "test-api-key",
                  "senderId": "DARSMS",
                  "isActive": true,
                  "monthlyLimit": 700,
                  "dailyLimit": 45,
                  "remarks": "to all people"
                }
                """;

        CreateSmsConfigurationRequest req = mapper.readValue(json, CreateSmsConfigurationRequest.class);

        assertEquals("DARSMS", req.getSenderId());
        assertNull(req.getApiSecret());
    }

    @Test
    void shouldDeserializeWithEmptyLimits() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = """
                {
                  "schoolId": 1,
                  "provider": "DARSMS",
                  "apiKey": "test-api-key",
                  "senderId": "DARSMS",
                  "isActive": true,
                  "remarks": "to all people"
                }
                """;

        CreateSmsConfigurationRequest req = mapper.readValue(json, CreateSmsConfigurationRequest.class);

        assertNull(req.getMonthlyLimit());
        assertNull(req.getDailyLimit());
    }

    @Test
    void shouldFailOnTrulyUnrecognizedField() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        String json = """
                {
                  "schoolId": 1,
                  "provider": "DARSMS",
                  "apiKey": "test-api-key",
                  "senderId": "DARSMS",
                  "isActive": true,
                  "monthlyLimit": 700,
                  "dailyLimit": 45,
                  "remarks": "to all people",
                  "schoolName": "mbuyuni sec school"
                }
                """;

        Exception ex = assertThrows(Exception.class,
                () -> mapper.readValue(json, CreateSmsConfigurationRequest.class));

        assertTrue(ex.getMessage().contains("Unrecognized field"),
                "Expected UnrecognizedFieldException, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("schoolName"),
                "Expected field name 'schoolName' in error, got: " + ex.getMessage());
    }
}

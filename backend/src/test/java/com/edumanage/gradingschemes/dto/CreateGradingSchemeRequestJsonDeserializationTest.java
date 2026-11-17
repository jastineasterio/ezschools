package com.edumanage.gradingschemes.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreateGradingSchemeRequestJsonDeserializationTest {

    @Test
    void shouldDeserializeIsDefaultField() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = """
                {
                  "name": "O Level Grading",
                  "description": "Secondary grading",
                  "isDefault": true,
                  "status": "ACTIVE"
                }
                """;

        CreateGradingSchemeRequest req = mapper.readValue(json, CreateGradingSchemeRequest.class);

        assertEquals("O Level Grading", req.getName());
        assertEquals("Secondary grading", req.getDescription());
        assertTrue(req.isDefault());
        assertEquals("ACTIVE", req.getStatus());
    }

    @Test
    void shouldDefaultIsDefaultToFalseWhenOmitted() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = """
                {
                  "name": "O Level Grading",
                  "status": "ACTIVE"
                }
                """;

        CreateGradingSchemeRequest req = mapper.readValue(json, CreateGradingSchemeRequest.class);

        assertEquals("O Level Grading", req.getName());
        assertFalse(req.isDefault());
        assertEquals("ACTIVE", req.getStatus());
    }

    @Test
    void shouldFailOnTrulyUnrecognizedField() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        assertTrue(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        String json = """
                {
                  "name": "O Level Grading",
                  "isDefault": true,
                  "status": "ACTIVE",
                  "unknownField": "should be rejected"
                }
                """;

        Exception ex = assertThrows(Exception.class,
                () -> mapper.readValue(json, CreateGradingSchemeRequest.class));

        assertTrue(ex.getMessage().contains("Unrecognized field"),
                "Expected Unrecognized field error, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("unknownField"),
                "Expected field name 'unknownField' in error, got: " + ex.getMessage());
    }
}

package com.edumanage.packages.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatePackageRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesActiveAndDefaultProperties() throws Exception {
        var request = objectMapper.readValue("""
                {
                  "name": "starter",
                  "displayName": "Starter",
                  "price": 10.00,
                  "billingPeriod": "MONTHLY",
                  "active": false,
                  "default": true
                }
                """, CreatePackageRequest.class);

        assertFalse(request.isActive());
        assertTrue(request.isDefault());
    }

    @Test
    void rejectsUnsupportedIsActiveProperty() {
        assertThrows(UnrecognizedPropertyException.class, () -> objectMapper.readValue("""
                {
                  "name": "starter",
                  "displayName": "Starter",
                  "price": 10.00,
                  "billingPeriod": "MONTHLY",
                  "isActive": false
                }
                """, CreatePackageRequest.class));
    }

    @Test
    void serializesPackageStatusUsingBackendPropertyNames() throws Exception {
        var response = new PackageDto();
        response.setActive(true);
        response.setDefault(true);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals(true, json.get("active").booleanValue());
        assertEquals(true, json.get("default").booleanValue());
        assertFalse(json.has("isActive"));
        assertFalse(json.has("isDefault"));
    }
}

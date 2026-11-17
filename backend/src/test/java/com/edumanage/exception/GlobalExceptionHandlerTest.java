package com.edumanage.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void returnsBadRequestForUnreadableRequestBody() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/platform/packages");
        var exception = new HttpMessageNotReadableException(
                "Could not read JSON",
                new MockHttpInputMessage(new byte[0]));

        var response = handler.handleUnreadableMessage(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Malformed request body", body.get("message"));
        assertEquals("/api/platform/packages", body.get("path"));
    }

    @Test
    void translatesUniqueConstraintViolationToConflict() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/school/teacher-assignments");
        SQLException sqlException = new SQLException("duplicate key value violates unique constraint", "23505");
        var exception = new DataIntegrityViolationException("Duplicate entry", sqlException);

        var response = handler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        var body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertTrue(((String) body.get("message")).contains("already exists"));
        assertEquals("/api/school/teacher-assignments", body.get("path"));
    }

    @Test
    void translatesNonUniqueDataIntegrityViolationToBadRequest() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/school/teacher-assignments");
        SQLException sqlException = new SQLException("null value in column", "23502");
        var exception = new DataIntegrityViolationException("Not null violation", sqlException);

        var response = handler.handleDataIntegrityViolation(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}

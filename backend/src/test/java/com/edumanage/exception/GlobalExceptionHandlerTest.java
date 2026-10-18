package com.edumanage.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}

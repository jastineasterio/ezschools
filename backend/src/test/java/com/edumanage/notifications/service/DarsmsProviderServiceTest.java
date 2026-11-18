package com.edumanage.notifications.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
class DarsmsProviderServiceTest {

    private static RestClient.Builder builderStub() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(RestClient.class));
        return builder;
    }

    @Test
    void isConfiguredWhenApiKeyAndSenderIdPresent() {
        DarsmsProviderService provider = new DarsmsProviderService(
                builderStub(), "api-key-123", "EDUMANGE", "https://dev.darsms.co.tz/api/v1");
        assertTrue(provider.isConfigured());
    }

    @Test
    void isConfiguredDoesNotRequireBaseUrl() {
        DarsmsProviderService provider = new DarsmsProviderService(
                builderStub(), "api-key-123", "EDUMANGE", "");
        assertTrue(provider.isConfigured(), "base-url is optional and must not disable a working provider");
    }

    @Test
    void isNotConfiguredWhenApiKeyMissing() {
        DarsmsProviderService provider = new DarsmsProviderService(
                builderStub(), "", "EDUMANGE", "https://dev.darsms.co.tz/api/v1");
        assertFalse(provider.isConfigured());
    }

    @Test
    void isNotConfiguredWhenSenderIdMissing() {
        DarsmsProviderService provider = new DarsmsProviderService(
                builderStub(), "api-key-123", "", "https://dev.darsms.co.tz/api/v1");
        assertFalse(provider.isConfigured());
    }

    @Test
    void isNotConfiguredWhenBothMissing() {
        DarsmsProviderService provider = new DarsmsProviderService(
                builderStub(), "", "", "https://dev.darsms.co.tz/api/v1");
        assertFalse(provider.isConfigured());
    }

    @Test
    void normalizesTanzanianPhones() {
        assertEquals("255712345678", DarsmsProviderService.normalizeTanzanianPhone("255712345678"));
        assertEquals("255712345678", DarsmsProviderService.normalizeTanzanianPhone("+255712345678"));
        assertEquals("255712345678", DarsmsProviderService.normalizeTanzanianPhone("0712345678"));
        assertEquals("255712345678", DarsmsProviderService.normalizeTanzanianPhone("0712 345-678"));
    }

    @Test
    void rejectsInvalidPhones() {
        assertThrows(NotificationProviderException.class, () -> DarsmsProviderService.normalizeTanzanianPhone(null));
        assertThrows(NotificationProviderException.class, () -> DarsmsProviderService.normalizeTanzanianPhone(""));
        assertThrows(NotificationProviderException.class, () -> DarsmsProviderService.normalizeTanzanianPhone("12345"));
        assertThrows(NotificationProviderException.class, () -> DarsmsProviderService.normalizeTanzanianPhone("0712"));
    }

    @Test
    void getApprovedSenderIdsReturnsOnlyApprovedSenderIds() {
        Map<String, Object> sender1 = Map.of("id", "id-1", "sender_id", "DARSMS", "status", "APPROVED");
        Map<String, Object> sender2 = Map.of("id", "id-2", "sender_id", "NEWGENESIS", "status", "APPROVED");
        Map<String, Object> sender3 = Map.of("id", "id-3", "sender_id", "PENDING_ID", "status", "PENDING");
        Map<String, Object> sender4 = Map.of("id", "id-4", "status", "APPROVED");
        Map<String, Object> sender5 = Map.of("id", "id-5", "sender_id", "LOWERCASE", "status", "approved");

        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Approved sender IDs",
                "data", List.of(sender1, sender2, sender3, sender4, sender5)
        );

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "test-api-key", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );

        List<String> result = service.getApprovedSenderIds("test-api-key");

        assertEquals(List.of("DARSMS", "NEWGENESIS", "LOWERCASE"), result);
    }

    @Test
    void getApprovedSenderIdsRejectsUnsuccessfulResponse() {
        Map<String, Object> response = Map.of(
                "success", false,
                "message", "Invalid API key",
                "code", "APP_ERROR"
        );

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "test-api-key", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );

        assertThrows(NotificationProviderException.class,
                () -> service.getApprovedSenderIds("test-api-key"));
    }

    @Test
    void getApprovedSenderIdsHandlesNonListData() {
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Approved sender IDs",
                "data", "not-a-list"
        );

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "test-api-key", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );

        List<String> result = service.getApprovedSenderIds("test-api-key");

        assertTrue(result.isEmpty());
    }

    @Test
    void getApprovedSenderIdsHandlesNullData() {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Approved sender IDs");
        response.put("data", null);

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "test-api-key", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );

        List<String> result = service.getApprovedSenderIds("test-api-key");

        assertTrue(result.isEmpty());
    }
}

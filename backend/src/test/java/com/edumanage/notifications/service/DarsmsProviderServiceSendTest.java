package com.edumanage.notifications.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
class DarsmsProviderServiceSendTest {

    @Test
    void extractMessageIdFromMessageIds() throws Exception {
        Method m = DarsmsProviderService.class.getDeclaredMethod("extractMessageId", Map.class);
        m.setAccessible(true);
        Map<String, Object> result = Map.of("messageIds", List.of("msg-123"));
        assertEquals("msg-123", m.invoke(null, result));
    }

    @Test
    void extractMessageIdFromSingleMessageId() throws Exception {
        Method m = DarsmsProviderService.class.getDeclaredMethod("extractMessageId", Map.class);
        m.setAccessible(true);
        Map<String, Object> result = Map.of("messageId", "single-msg");
        assertEquals("single-msg", m.invoke(null, result));
    }

    @Test
    void extractMessageIdReturnsNullWhenAbsent() throws Exception {
        Method m = DarsmsProviderService.class.getDeclaredMethod("extractMessageId", Map.class);
        m.setAccessible(true);
        assertNull(m.invoke(null, Map.of("batchId", "b")));
        assertNull(m.invoke(null, Map.of()));
        assertNull(m.invoke(null, Map.of("messageIds", List.of())));
    }

    @Test
    void sendRejectedThrowsProviderRejected() {
        Map<String, Object> response = Map.of(
                "success", false,
                "message", "Rejected",
                "data", Map.of("messageIds", List.of("msg-123"))
        );
        RestClient restClient = restClientMock(response);

        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "test-api-key", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );

        assertThrows(NotificationProviderException.class,
                () -> service.send("0712345678", "hello", "test-api-key", "test-sender"));
    }

    @Test
    void sendMissingConfigThrowsNotConfigured() {
        RestClient restClient = mock(RestClient.class);
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        DarsmsProviderService service = new DarsmsProviderService(
                builder, "", "test-sender-id", "https://dev.darsms.co.tz/api/v1"
        );
        assertThrows(NotificationProviderException.class,
                () -> service.send("0712345678", "hello", "", "test-sender"));
    }

    private static RestClient restClientMock(Map<String, Object> response) {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec post = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(post);
        when(post.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(Map.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(response);
        return restClient;
    }
}

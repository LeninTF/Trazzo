package trazzo.back.corehr.infrastructure.adapters.out.notification;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

class ResendAttendanceNotificationAdapterTest {

    private static void injectRestClient(ResendAttendanceNotificationAdapter adapter, RestClient restClient) throws Exception {
        var field = ResendAttendanceNotificationAdapter.class.getDeclaredField("restClient");
        field.setAccessible(true);
        field.set(adapter, restClient);
    }

    @Test
    void notifyTardanza_shouldSkipWhenApiKeyIsBlank() {
        var adapter = new ResendAttendanceNotificationAdapter("", "from@test.com");

        adapter.notifyTardanza("to@test.com", "Juan Perez", 15, LocalDate.of(2025, 1, 15));
    }

    @Test
    void notifyTardanza_shouldSkipWhenApiKeyIsNull() {
        var adapter = new ResendAttendanceNotificationAdapter(null, "from@test.com");

        adapter.notifyTardanza("to@test.com", "Juan Perez", 15, LocalDate.of(2025, 1, 15));
    }

    @Test
    void notifyTardanza_shouldSkipWhenToEmailIsNull() {
        var adapter = new ResendAttendanceNotificationAdapter("re_key_xxx", "from@test.com");

        adapter.notifyTardanza(null, "Juan Perez", 15, LocalDate.of(2025, 1, 15));
    }

    @Test
    void notifyTardanza_shouldSendEmailWhenProperlyConfigured() throws Exception {
        var adapter = new ResendAttendanceNotificationAdapter("re_key_xxx", "from@test.com");
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, org.mockito.Answers.RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        injectRestClient(adapter, restClient);

        assertDoesNotThrow(() -> adapter.notifyTardanza("to@test.com", "Juan Perez", 15, LocalDate.of(2025, 1, 15)));

        verify(restClient).post();
        verify(bodyUriSpec).header("Authorization", "Bearer re_key_xxx");
    }

    @Test
    void notifyTardanza_shouldHandleSendFailureGracefully() throws Exception {
        var adapter = new ResendAttendanceNotificationAdapter("re_key_xxx", "from@test.com");
        RestClient restClient = mock(RestClient.class);

        when(restClient.post()).thenThrow(new RuntimeException("Connection refused"));

        injectRestClient(adapter, restClient);

        assertDoesNotThrow(() -> adapter.notifyTardanza("to@test.com", "Juan Perez", 15, LocalDate.of(2025, 1, 15)));
    }

    @Test
    void notifyTardanza_shouldPassBodyWithCorrectFields() throws Exception {
        var adapter = new ResendAttendanceNotificationAdapter("re_key_xxx", "from@test.com");
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, org.mockito.Answers.RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        injectRestClient(adapter, restClient);

        assertDoesNotThrow(() -> adapter.notifyTardanza("to@test.com", "Maria Garcia", 30, LocalDate.of(2025, 6, 20)));

        verify(bodyUriSpec).body(anyMap());
    }

    @Test
    void notifyTardanza_shouldUseDefaultName_whenWorkerNameIsNull() throws Exception {
        var adapter = new ResendAttendanceNotificationAdapter("re_key_xxx", "from@test.com");
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, org.mockito.Answers.RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        injectRestClient(adapter, restClient);

        assertDoesNotThrow(() -> adapter.notifyTardanza("to@test.com", null, 15, LocalDate.of(2025, 1, 15)));

        verify(bodyUriSpec).body(anyMap());
    }
}

package trazzo.back.saasglobal.infrastructure.adapters.out.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ResendEmailAdapterTest {

    @Test
    void send_skipsHttpCallWhenApiKeyBlank() {
        RestClient.Builder builder = mock(RestClient.Builder.class, org.mockito.Answers.RETURNS_SELF);
        RestClient restClient = mock(RestClient.class);
        when(builder.build()).thenReturn(restClient);

        var adapter = new ResendEmailAdapter(builder, "", "notificaciones@trazzo.pe");

        assertDoesNotThrow(() -> adapter.send("to@example.com", "subject", "body"));
        verify(restClient, never()).post();
    }

    @Test
    void send_skipsHttpCallWhenApiKeyNull() {
        RestClient.Builder builder = mock(RestClient.Builder.class, org.mockito.Answers.RETURNS_SELF);
        RestClient restClient = mock(RestClient.class);
        when(builder.build()).thenReturn(restClient);

        var adapter = new ResendEmailAdapter(builder, null, "notificaciones@trazzo.pe");

        assertDoesNotThrow(() -> adapter.send("to@example.com", "subject", "body"));
        verify(restClient, never()).post();
    }

    @Test
    void send_attemptsHttpCallWhenApiKeyConfigured() {
        RestClient.Builder builder = mock(RestClient.Builder.class, org.mockito.Answers.RETURNS_SELF);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, org.mockito.Answers.RETURNS_SELF);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        var adapter = new ResendEmailAdapter(builder, "re_test_key", "notificaciones@trazzo.pe");

        assertDoesNotThrow(() -> adapter.send("to@example.com", "subject", "body"));
        verify(restClient).post();
        verify(bodyUriSpec).header(eq("Authorization"), anyString());
    }


    @Test
    void send_swallowsExceptionWhenHttpCallFails() {
        RestClient.Builder builder = mock(RestClient.Builder.class, org.mockito.Answers.RETURNS_SELF);
        RestClient restClient = mock(RestClient.class);

        when(builder.build()).thenReturn(restClient);
        when(restClient.post()).thenThrow(new RuntimeException("connection refused"));

        var adapter = new ResendEmailAdapter(builder, "re_test_key", "notificaciones@trazzo.pe");

        assertDoesNotThrow(() -> adapter.send("to@example.com", "subject", "body"));
    }
}

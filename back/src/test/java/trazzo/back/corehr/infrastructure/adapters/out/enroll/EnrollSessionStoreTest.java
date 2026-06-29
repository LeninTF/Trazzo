package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollSessionStoreTest {

    private EnrollSessionStore store;

    @BeforeEach
    void setUp() {
        store = new EnrollSessionStore();
    }

    @Test
    void createAndFindSession() {
        var expires = LocalDateTime.now().plusSeconds(120);
        var session = new EnrollSession("token-1", 10L, 5L, 3, "DVC-001", expires);
        store.createSession(session);

        var found = store.findAndConsume("token-1");

        assertThat(found).isNotNull();
        assertThat(found.enrollToken()).isEqualTo("token-1");
        assertThat(found.tenantUserId()).isEqualTo(10L);
    }

    @Test
    void findAndConsumeRemovesSession() {
        var expires = LocalDateTime.now().plusSeconds(120);
        store.createSession(new EnrollSession("token-2", 20L, 7L, 1, "DVC-002", expires));

        store.findAndConsume("token-2");
        var secondLookup = store.findAndConsume("token-2");

        assertThat(secondLookup).isNull();
    }

    @Test
    void findAndConsumeReturnsNullForUnknownToken() {
        var result = store.findAndConsume("nonexistent");

        assertThat(result).isNull();
    }

    @Test
    void findAndConsumeReturnsNullForExpiredSession() {
        var expires = LocalDateTime.now().minusSeconds(1);
        store.createSession(new EnrollSession("expired", 10L, 5L, 2, "DVC", expires));

        var result = store.findAndConsume("expired");

        assertThat(result).isNull();
    }

    @Test
    void existsActiveSessionReturnsTrueWhenActive() {
        var expires = LocalDateTime.now().plusSeconds(60);
        store.createSession(new EnrollSession("token", 10L, 5L, 3, "DVC", expires));

        var exists = store.existsActiveSession(10L, 5L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsActiveSessionReturnsFalseWhenNoSession() {
        var exists = store.existsActiveSession(99L, 99L);

        assertThat(exists).isFalse();
    }

    @Test
    void existsActiveSessionReturnsFalseWhenSessionExpired() {
        var expires = LocalDateTime.now().minusSeconds(1);
        store.createSession(new EnrollSession("expired", 10L, 5L, 3, "DVC", expires));

        var exists = store.existsActiveSession(10L, 5L);

        assertThat(exists).isFalse();
    }

    @Test
    void existsActiveSessionIgnoresDifferentUserOrDevice() {
        var expires = LocalDateTime.now().plusSeconds(60);
        store.createSession(new EnrollSession("token", 10L, 5L, 3, "DVC", expires));

        assertThat(store.existsActiveSession(10L, 99L)).isFalse();
        assertThat(store.existsActiveSession(99L, 5L)).isFalse();
    }

    @Test
    void multipleSessionsForDifferentUsers() {
        var expires = LocalDateTime.now().plusSeconds(60);
        store.createSession(new EnrollSession("t1", 1L, 1L, 0, "D1", expires));
        store.createSession(new EnrollSession("t2", 2L, 2L, 0, "D2", expires));

        assertThat(store.existsActiveSession(1L, 1L)).isTrue();
        assertThat(store.existsActiveSession(2L, 2L)).isTrue();

        assertThat(store.findAndConsume("t1")).isNotNull();
        assertThat(store.findAndConsume("t2")).isNotNull();
        assertThat(store.findAndConsume("t1")).isNull();
    }
}

package trazzo.back.saasglobal.domain.model.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserRequestCommentTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        UserRequestComment urc = UserRequestComment.create("user-1", 42);
        var after = LocalDateTime.now();

        assertNull(urc.getId());
        assertEquals("user-1", urc.getUserId());
        assertEquals(42, urc.getRequestCommentId());
        assertFalse(urc.getCreatedAt().isBefore(before));
        assertFalse(urc.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        UserRequestComment urc = UserRequestComment.restore(5, "user-2", 99, now);

        assertEquals(5, urc.getId());
        assertEquals("user-2", urc.getUserId());
        assertEquals(99, urc.getRequestCommentId());
        assertEquals(now, urc.getCreatedAt());
    }

    @Test
    void create_throwsWhenUserIdBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                UserRequestComment.create("", 42));
    }

    @Test
    void create_throwsWhenUserIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                UserRequestComment.create(null, 42));
    }

    @Test
    void create_throwsWhenRequestCommentIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                UserRequestComment.create("user-1", null));
    }
}

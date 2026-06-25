package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TenantSettingsTest {

    @Test
    void of_setsAllFields() {
        var before = LocalDateTime.now();
        var s = TenantSettings.of("t-1", "localhost", "5432", "mydb", "user", "pass");
        var after = LocalDateTime.now();

        assertEquals("t-1", s.getTenantId());
        assertEquals("localhost", s.getDbHost());
        assertEquals("5432", s.getDbPort());
        assertEquals("mydb", s.getDbName());
        assertEquals("user", s.getDbUser());
        assertEquals("pass", s.getDbPassword());
        assertFalse(s.getCreatedAt().isBefore(before));
        assertFalse(s.getCreatedAt().isAfter(after));
    }

    @Test
    void of_nullTenantIdIsAllowed() {
        assertDoesNotThrow(() -> TenantSettings.of(null, "host", "5432", "db", "u", "p"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenDbHostBlank(String host) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", host, "5432", "db", "u", "p"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenDbPortBlank(String port) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", "host", port, "db", "u", "p"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenDbNameBlank(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", "host", "5432", name, "u", "p"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenDbUserBlank(String user) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", "host", "5432", "db", user, "p"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void of_throwsWhenDbPasswordBlank(String pass) {
        assertThrows(IllegalArgumentException.class,
                () -> TenantSettings.of("t-1", "host", "5432", "db", "u", pass));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var s = TenantSettings.restore("t-2", "mydb", "remotehost", "5433", "admin", "secret", now, now);

        assertEquals("t-2", s.getTenantId());
        assertEquals("mydb", s.getDbName());
        assertEquals("remotehost", s.getDbHost());
        assertEquals("5433", s.getDbPort());
        assertEquals("admin", s.getDbUser());
        assertEquals("secret", s.getDbPassword());
        assertEquals(now, s.getCreatedAt());
        assertEquals(now, s.getUpdatedAt());
    }

    @Test
    void rotatePassword_updatesPassword() {
        var s = TenantSettings.of("t-1", "host", "5432", "db", "user", "oldpass");
        s.rotatePassword("newpass");
        assertEquals("newpass", s.getDbPassword());
    }

    @Test
    void rotatePassword_throwsWhenBlank() {
        var s = TenantSettings.of("t-1", "host", "5432", "db", "user", "pass");
        assertThrows(IllegalArgumentException.class, () -> s.rotatePassword("  "));
    }
}

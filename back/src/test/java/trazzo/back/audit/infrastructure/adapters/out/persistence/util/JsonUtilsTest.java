package trazzo.back.audit.infrastructure.adapters.out.persistence.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilsTest {

    @Test
    void deserialize_returnsEmptyMapForNull() {
        assertThat(JsonUtils.deserialize(null)).isEmpty();
    }

    @Test
    void deserialize_returnsEmptyMapForBlank() {
        assertThat(JsonUtils.deserialize("  ")).isEmpty();
    }

    @Test
    void deserialize_returnsEmptyMapForInvalidJson() {
        assertThat(JsonUtils.deserialize("not json")).isEmpty();
    }

    @Test
    void deserialize_returnsMapForValidJson() {
        var result = JsonUtils.deserialize("{\"key\":\"value\",\"num\":42}");
        assertThat(result).containsEntry("key", "value");
        assertThat(result).containsEntry("num", 42);
    }

    @Test
    void deserialize_returnsEmptyMapForEmptyObject() {
        assertThat(JsonUtils.deserialize("{}")).isEmpty();
    }

    @Test
    void serialize_returnsNullForNull() {
        assertThat(JsonUtils.serialize(null)).isNull();
    }

    @Test
    void serialize_returnsNullForEmptyMap() {
        assertThat(JsonUtils.serialize(Map.of())).isNull();
    }

    @Test
    void serialize_returnsJsonStringForValidMap() {
        var result = JsonUtils.serialize(Map.of("key", "value"));
        assertThat(result).isNotBlank();
        assertThat(result).contains("key").contains("value");
    }

    @Test
    void serialize_handlesNestedMap() {
        var result = JsonUtils.serialize(Map.of("outer", Map.of("inner", "val")));
        assertThat(result).isNotBlank();
        assertThat(result).contains("inner");
    }
}

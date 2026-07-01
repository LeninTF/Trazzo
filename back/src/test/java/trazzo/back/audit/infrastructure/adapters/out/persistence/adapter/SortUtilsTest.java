package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SortUtilsTest {

    private final Function<String, String> fieldMapper = Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "entity", "entity"
    )::get;

    @Test
    void shouldReturnDefaultSortWhenNull() {
        var sort = SortUtils.parseSort(null, fieldMapper);

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    void shouldReturnDefaultSortWhenBlank() {
        var sort = SortUtils.parseSort("  ", fieldMapper);

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    void shouldParseAscDirection() {
        var sort = SortUtils.parseSort("entity,asc", fieldMapper);

        assertEquals(Sort.Direction.ASC, sort.getOrderFor("entity").getDirection());
    }

    @Test
    void shouldParseDescDirection() {
        var sort = SortUtils.parseSort("updatedAt,desc", fieldMapper);

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("updatedAt").getDirection());
    }

    @Test
    void shouldDefaultToAscWhenNoDirectionProvided() {
        var sort = SortUtils.parseSort("createdAt", fieldMapper);

        assertEquals(Sort.Direction.ASC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    void shouldUseFieldMapper() {
        var sort = SortUtils.parseSort("entity,desc", fieldMapper);

        assertEquals("entity", sort.getOrderFor("entity").getProperty());
    }

    @Test
    void shouldThrowWhenMapperReturnsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> SortUtils.parseSort("unknown,asc", s -> null));
    }
}

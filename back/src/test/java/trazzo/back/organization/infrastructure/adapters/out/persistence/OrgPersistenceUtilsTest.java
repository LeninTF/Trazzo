package trazzo.back.organization.infrastructure.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class OrgPersistenceUtilsTest {

    @Test
    void parseSort_null_returnsDefaultAscByName() {
        Sort sort = OrgPersistenceUtils.parseSort(null);
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_blank_returnsDefaultAscByName() {
        Sort sort = OrgPersistenceUtils.parseSort("   ");
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_nameAsc_returnsAscByName() {
        Sort sort = OrgPersistenceUtils.parseSort("name,asc");
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void parseSort_nameDesc_returnsDescByName() {
        Sort sort = OrgPersistenceUtils.parseSort("name,desc");
        assertThat(sort.getOrderFor("name")).isNotNull();
        assertThat(sort.getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_createdAtSnakeCase_mapsToCreatedAt() {
        Sort sort = OrgPersistenceUtils.parseSort("created_at,asc");
        assertThat(sort.getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void parseSort_updatedAtCamelCase_mapsToUpdatedAt() {
        Sort sort = OrgPersistenceUtils.parseSort("updatedAt,desc");
        assertThat(sort.getOrderFor("updatedAt")).isNotNull();
        assertThat(sort.getOrderFor("updatedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void parseSort_updatedAtSnakeCase_mapsToUpdatedAt() {
        Sort sort = OrgPersistenceUtils.parseSort("updated_at");
        assertThat(sort.getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void parseSort_unknownField_defaultsToName() {
        Sort sort = OrgPersistenceUtils.parseSort("unknown,asc");
        assertThat(sort.getOrderFor("name")).isNotNull();
    }

    @Test
    void blankToNull_null_returnsNull() {
        assertThat(OrgPersistenceUtils.blankToNull(null)).isNull();
    }

    @Test
    void blankToNull_blankString_returnsNull() {
        assertThat(OrgPersistenceUtils.blankToNull("   ")).isNull();
    }

    @Test
    void blankToNull_nonBlankString_returnsValue() {
        assertThat(OrgPersistenceUtils.blankToNull("hello")).isEqualTo("hello");
    }

    @Test
    void mapField_createdAtSnakeCase_mapsToCreatedAt() {
        assertThat(OrgPersistenceUtils.mapField("created_at")).isEqualTo("createdAt");
    }

    @Test
    void mapField_createdAtCamelCase_mapsToCreatedAt() {
        assertThat(OrgPersistenceUtils.mapField("createdAt")).isEqualTo("createdAt");
    }

    @Test
    void mapField_updatedAtSnakeCase_mapsToUpdatedAt() {
        assertThat(OrgPersistenceUtils.mapField("updated_at")).isEqualTo("updatedAt");
    }

    @Test
    void mapField_unknownField_defaultsToName() {
        assertThat(OrgPersistenceUtils.mapField("other")).isEqualTo("name");
    }

    @Test
    void likePattern_null_returnsNull() {
        assertThat(OrgPersistenceUtils.likePattern(null)).isNull();
    }

    @Test
    void likePattern_blankString_returnsNull() {
        assertThat(OrgPersistenceUtils.likePattern("   ")).isNull();
    }

    @Test
    void likePattern_nonBlankString_wrapsWithWildcardsAndLowercases() {
        assertThat(OrgPersistenceUtils.likePattern("HeLLo")).isEqualTo("%hello%");
    }
}

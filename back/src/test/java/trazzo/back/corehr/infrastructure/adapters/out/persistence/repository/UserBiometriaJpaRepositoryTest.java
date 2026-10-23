package trazzo.back.corehr.infrastructure.adapters.out.persistence.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Transactional
class UserBiometriaJpaRepositoryTest {

    @Autowired
    private UserBiometriaJpaRepository repository;

    private UserBiometriaEntity save(long tenantUserId, int fingerIndex, boolean activo) {
        var e = new UserBiometriaEntity();
        e.setTenantUserId(tenantUserId);
        e.setFingerIndex(fingerIndex);
        e.setEncryptedTemplateBase64("c2VsbGFkbw==");
        e.setIvBase64("aXY=");
        e.setTagBase64("dGFn");
        e.setKeyRef("storage-v1");
        e.setCapturadoEn(LocalDateTime.now());
        e.setActivo(activo);
        return repository.saveAndFlush(e);
    }

    @Test
    void findsEveryActiveRowOfAFingerAndNoInactiveOnes() {
        var old = save(10L, 1, false);
        var reactivated = save(10L, 1, true);
        var current = save(10L, 1, true);
        save(10L, 2, true);
        save(11L, 1, true);

        var rows = repository.findByTenantUserIdAndFingerIndexAndActivoTrue(10L, 1);

        assertThat(rows).extracting(UserBiometriaEntity::getId)
                .containsExactlyInAnyOrder(reactivated.getId(), current.getId())
                .doesNotContain(old.getId());
    }

    @Test
    void pagesActiveRowsInIdOrderAfterTheLastSeenId() {
        var a = save(1L, 1, true);
        save(2L, 1, false);
        var c = save(3L, 1, true);
        var d = save(4L, 1, true);

        List<UserBiometriaEntity> first = repository.findActiveAfterId(0L, PageRequest.ofSize(2));
        List<UserBiometriaEntity> second = repository.findActiveAfterId(first.getLast().getId(), PageRequest.ofSize(2));

        assertThat(first).extracting(UserBiometriaEntity::getId).containsExactly(a.getId(), c.getId());
        assertThat(second).extracting(UserBiometriaEntity::getId).containsExactly(d.getId());
    }

    @Test
    void storesASealedRowWithItsKeyRefAndNoWrappedKey() {
        var saved = save(20L, 3, true);

        var reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getKeyRef()).isEqualTo("storage-v1");
        assertThat(reloaded.getEncryptedAesKeyBase64()).isNull();
    }
}

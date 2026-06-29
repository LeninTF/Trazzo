package trazzo.back.corehr.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseDomainModel {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    protected transient Clock clock = Clock.systemDefaultZone();

    protected BaseDomainModel(Long id, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    protected void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }
}

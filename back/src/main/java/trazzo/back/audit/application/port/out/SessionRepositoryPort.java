package trazzo.back.audit.application.port.out;

import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface SessionRepositoryPort {
    List<Session> findAll(String tenantUserId, SessionState state, String ipAddress, Pageable pageable);
    long count(String tenantUserId, SessionState state, String ipAddress);
    Optional<Session> findById(Long id);
}

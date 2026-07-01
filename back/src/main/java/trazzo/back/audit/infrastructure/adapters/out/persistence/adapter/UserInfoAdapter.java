package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.UserInfoPort;

import java.util.Optional;

@Component
public class UserInfoAdapter implements UserInfoPort {

    @Override
    public Optional<UserInfo> findByUserId(String userId) {
        return Optional.empty();
    }
}

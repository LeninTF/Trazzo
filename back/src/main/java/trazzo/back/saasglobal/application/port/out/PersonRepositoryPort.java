package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.Person;

public interface PersonRepositoryPort {
    Optional<Person> findById(Integer id);
}

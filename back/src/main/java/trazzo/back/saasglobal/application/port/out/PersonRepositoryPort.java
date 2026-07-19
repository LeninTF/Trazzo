package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.Person;

public interface PersonRepositoryPort {
    Optional<Person> findById(Integer id);
    Person save(Person person);

    /**
     * Hard delete — used only to compensate a checkout that failed before any payment attempt.
     * Cascades to delete the owning {@code users} row (FK {@code users.person_id ON DELETE CASCADE}).
     */
    void deleteById(Integer id);
}

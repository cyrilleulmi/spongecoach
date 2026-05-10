package coach.spongecoach.person.domain;

import coach.spongecoach.person.domain.model.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {
    Person save(Person person);
    Optional<Person> findById(UUID id);
    List<Person> findAll();
    boolean existsByEmail(String email);
    void delete(UUID id);
}

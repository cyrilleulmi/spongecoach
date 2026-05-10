package coach.spongecoach.person.adapter.out.persistence;

import coach.spongecoach.person.domain.model.Person;
import org.springframework.stereotype.Component;

@Component
class PersonMapper {

    Person toDomain(PersonJpaEntity entity) {
        return new Person(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail());
    }

    PersonJpaEntity toEntity(Person person) {
        return new PersonJpaEntity(person.id(), person.firstName(), person.lastName(), person.email());
    }
}

package coach.spongecoach.person.application;

import coach.spongecoach.person.domain.DeletePersonGuard;
import coach.spongecoach.person.domain.PersonRepository;
import coach.spongecoach.person.domain.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonServiceTest {

    private PersonService serviceWithNoGuard;
    private PersonService serviceBlockedByGuard;
    private InMemoryPersonRepository personRepo;

    @BeforeEach
    void setUp() {
        personRepo = new InMemoryPersonRepository();
        serviceWithNoGuard = new PersonService(personRepo, personId -> List.of());
        serviceBlockedByGuard = new PersonService(personRepo, personId -> List.of("UHC Malters"));
    }

    @Test
    void createPerson_persistsAndReturnsPerson() {
        Person person = serviceWithNoGuard.createPerson("Alice", "Smith", "alice@test.ch");
        assertThat(person.id()).isNotNull();
        assertThat(person.firstName()).isEqualTo("Alice");
        assertThat(person.email()).isEqualTo("alice@test.ch");
    }

    @Test
    void createPerson_throwsOnDuplicateEmail() {
        serviceWithNoGuard.createPerson("Alice", "Smith", "alice@test.ch");
        assertThatThrownBy(() -> serviceWithNoGuard.createPerson("Alice2", "Smith2", "alice@test.ch"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void getPerson_throwsWhenNotFound() {
        assertThatThrownBy(() -> serviceWithNoGuard.getPerson(UUID.randomUUID()))
                .isInstanceOf(PersonNotFoundException.class);
    }

    @Test
    void deletePerson_removesPerson() {
        Person person = serviceWithNoGuard.createPerson("Bob", "Jones", "bob@test.ch");
        serviceWithNoGuard.deletePerson(person.id());
        assertThatThrownBy(() -> serviceWithNoGuard.getPerson(person.id()))
                .isInstanceOf(PersonNotFoundException.class);
    }

    @Test
    void deletePerson_blockedWhenLastAdminOfClub() {
        Person person = serviceWithNoGuard.createPerson("Charlie", "Sole", "charlie@test.ch");
        assertThatThrownBy(() -> serviceBlockedByGuard.deletePerson(person.id()))
                .isInstanceOf(LastClubAdminException.class)
                .hasMessageContaining("UHC Malters");
    }

    @Test
    void deletePerson_allowedWhenGuardReturnsEmpty() {
        Person person = serviceWithNoGuard.createPerson("Dave", "Safe", "dave@test.ch");
        serviceWithNoGuard.deletePerson(person.id());
        assertThatThrownBy(() -> serviceWithNoGuard.getPerson(person.id()))
                .isInstanceOf(PersonNotFoundException.class);
    }

    static class InMemoryPersonRepository implements PersonRepository {
        private final Map<UUID, Person> store = new HashMap<>();

        @Override
        public Person save(Person person) {
            store.put(person.id(), person);
            return person;
        }

        @Override
        public Optional<Person> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Person> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.values().stream().anyMatch(p -> p.email().equals(email));
        }

        @Override
        public void delete(UUID id) {
            store.remove(id);
        }
    }
}

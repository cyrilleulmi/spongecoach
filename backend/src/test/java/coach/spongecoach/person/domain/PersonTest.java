package coach.spongecoach.person.domain;

import coach.spongecoach.person.domain.model.Person;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonTest {

    @Test
    void create_assignsRandomId() {
        Person a = Person.create("Alice", "Smith", "alice@test.ch");
        Person b = Person.create("Alice", "Smith", "alice2@test.ch");
        assertThat(a.id()).isNotNull();
        assertThat(a.id()).isNotEqualTo(b.id());
    }

    @Test
    void withEmail_returnsNewInstanceWithUpdatedEmail() {
        Person original = Person.create("Bob", "Jones", "bob@test.ch");
        Person updated = original.withEmail("new@test.ch");
        assertThat(updated.email()).isEqualTo("new@test.ch");
        assertThat(updated.id()).isEqualTo(original.id());
    }
}

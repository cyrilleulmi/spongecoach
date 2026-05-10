package coach.spongecoach.person.domain.model;

import java.util.UUID;

public record Person(UUID id, String firstName, String lastName, String email) {

    public static Person create(String firstName, String lastName, String email) {
        return new Person(UUID.randomUUID(), firstName, lastName, email);
    }

    public Person withFirstName(String firstName) {
        return new Person(this.id, firstName, this.lastName, this.email);
    }

    public Person withLastName(String lastName) {
        return new Person(this.id, this.firstName, lastName, this.email);
    }

    public Person withEmail(String email) {
        return new Person(this.id, this.firstName, this.lastName, email);
    }
}

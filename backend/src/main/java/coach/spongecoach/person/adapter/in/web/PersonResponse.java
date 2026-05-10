package coach.spongecoach.person.adapter.in.web;

import coach.spongecoach.person.domain.model.Person;

import java.util.UUID;

record PersonResponse(UUID id, String firstName, String lastName, String email) {

    static PersonResponse from(Person person) {
        return new PersonResponse(person.id(), person.firstName(), person.lastName(), person.email());
    }
}

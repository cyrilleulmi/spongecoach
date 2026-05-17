package coach.spongecoach.person.application;

import coach.spongecoach.person.domain.DeletePersonGuard;
import coach.spongecoach.person.domain.PersonRepository;
import coach.spongecoach.person.domain.model.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final DeletePersonGuard deletePersonGuard;

    public PersonService(PersonRepository personRepository, DeletePersonGuard deletePersonGuard) {
        this.personRepository = personRepository;
        this.deletePersonGuard = deletePersonGuard;
    }

    public Person createPerson(String firstName, String lastName, String email) {
        if (personRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        return personRepository.save(Person.create(firstName, lastName, email));
    }

    @Transactional(readOnly = true)
    public Person getPerson(UUID id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }

    public Person updatePerson(UUID id, String firstName, String lastName, String email) {
        Person existing = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        if (!existing.email().equals(email) && personRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
        return personRepository.save(existing.withFirstName(firstName).withLastName(lastName).withEmail(email));
    }

    public void deletePerson(UUID id) {
        personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        List<String> blockingClubs = deletePersonGuard.findBlockingClubs(id);
        if (!blockingClubs.isEmpty()) {
            throw new LastClubAdminException(blockingClubs);
        }
        personRepository.delete(id);
    }
}

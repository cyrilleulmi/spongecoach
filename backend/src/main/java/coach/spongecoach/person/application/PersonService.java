package coach.spongecoach.person.application;

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

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
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

    @Transactional(readOnly = true)
    public List<Person> listPersons() {
        return personRepository.findAll();
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
        personRepository.delete(id);
    }
}

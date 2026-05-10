package coach.spongecoach.person.adapter.out.persistence;

import coach.spongecoach.person.domain.PersonRepository;
import coach.spongecoach.person.domain.model.Person;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PersonPersistenceAdapter implements PersonRepository {

    private final SpringDataPersonRepository springDataRepo;
    private final PersonMapper mapper;

    PersonPersistenceAdapter(SpringDataPersonRepository springDataRepo, PersonMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Person save(Person person) {
        return mapper.toDomain(springDataRepo.save(mapper.toEntity(person)));
    }

    @Override
    public Optional<Person> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Person> findAll() {
        return springDataRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepo.existsByEmail(email);
    }

    @Override
    public void delete(UUID id) {
        springDataRepo.deleteById(id);
    }
}

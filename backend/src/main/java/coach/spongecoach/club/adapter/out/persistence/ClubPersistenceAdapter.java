package coach.spongecoach.club.adapter.out.persistence;

import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.model.Club;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ClubPersistenceAdapter implements ClubRepository {

    private final SpringDataClubRepository springDataRepo;
    private final ClubMapper mapper;

    ClubPersistenceAdapter(SpringDataClubRepository springDataRepo, ClubMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Club save(Club club) {
        return mapper.toDomain(springDataRepo.save(mapper.toEntity(club)));
    }

    @Override
    public Optional<Club> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Club> findAll() {
        return springDataRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataRepo.existsById(id);
    }

    @Override
    public void delete(UUID id) {
        springDataRepo.deleteById(id);
    }
}

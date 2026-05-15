package coach.spongecoach.team.adapter.out.persistence;

import coach.spongecoach.team.domain.TeamRepository;
import coach.spongecoach.team.domain.model.Team;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class TeamPersistenceAdapter implements TeamRepository {

    private final SpringDataTeamRepository springDataRepo;
    private final TeamMapper mapper;

    TeamPersistenceAdapter(SpringDataTeamRepository springDataRepo, TeamMapper mapper) {
        this.springDataRepo = springDataRepo;
        this.mapper = mapper;
    }

    @Override
    public Team save(Team team) {
        return mapper.toDomain(springDataRepo.save(mapper.toEntity(team)));
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return springDataRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Team> findAll() {
        return springDataRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Team> findByClubId(UUID clubId) {
        return springDataRepo.findByClubId(clubId).stream().map(mapper::toDomain).toList();
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

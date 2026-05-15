package coach.spongecoach.team.domain;

import coach.spongecoach.team.domain.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findAll();
    List<Team> findByClubId(UUID clubId);
    boolean existsById(UUID id);
    void delete(UUID id);
}

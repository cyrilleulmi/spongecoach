package coach.spongecoach.club.domain;

import coach.spongecoach.club.domain.model.Club;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubRepository {
    Club save(Club club);
    Optional<Club> findById(UUID id);
    List<Club> findAll();
    boolean existsById(UUID id);
    void delete(UUID id);
}

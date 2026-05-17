package coach.spongecoach.club.domain;

import coach.spongecoach.auth.domain.model.ClubMembership;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubMembershipRepository {
    ClubMembership save(ClubMembership membership);
    Optional<ClubMembership> findByClubIdAndPersonId(UUID clubId, UUID personId);
    List<ClubMembership> findByPersonId(UUID personId);
    List<ClubMembership> findByClubId(UUID clubId);
    boolean existsByClubIdAndPersonId(UUID clubId, UUID personId);
    void delete(UUID clubId, UUID personId);
}

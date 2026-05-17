package coach.spongecoach.team.domain;

import coach.spongecoach.team.domain.model.TeamMembership;

import java.util.List;
import java.util.UUID;

public interface TeamMembershipRepository {
    TeamMembership save(TeamMembership membership);
    void delete(UUID teamId, UUID personId);
    void deleteByClubAndPerson(UUID clubId, UUID personId);
    List<TeamMembership> findByTeamId(UUID teamId);
    boolean exists(UUID teamId, UUID personId);
}

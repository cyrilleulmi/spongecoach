package coach.spongecoach.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataTeamMembershipRepository
        extends JpaRepository<TeamMembershipJpaEntity, TeamMembershipJpaEntity.TeamMembershipId> {

    List<TeamMembershipJpaEntity> findByIdTeamId(UUID teamId);

    boolean existsByIdTeamIdAndIdPersonId(UUID teamId, UUID personId);

    void deleteByIdTeamIdAndIdPersonId(UUID teamId, UUID personId);
}

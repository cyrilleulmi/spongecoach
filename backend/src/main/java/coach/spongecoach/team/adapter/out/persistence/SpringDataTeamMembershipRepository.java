package coach.spongecoach.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataTeamMembershipRepository
        extends JpaRepository<TeamMembershipJpaEntity, TeamMembershipJpaEntity.TeamMembershipId> {

    List<TeamMembershipJpaEntity> findByIdTeamId(UUID teamId);

    boolean existsByIdTeamIdAndIdPersonId(UUID teamId, UUID personId);

    void deleteByIdTeamIdAndIdPersonId(UUID teamId, UUID personId);

    @Modifying
    @Query("DELETE FROM TeamMembershipJpaEntity m WHERE m.id.personId = :personId AND m.id.teamId IN (SELECT t.id FROM TeamJpaEntity t WHERE t.clubId = :clubId)")
    void deleteByClubIdAndPersonId(@Param("clubId") UUID clubId, @Param("personId") UUID personId);
}

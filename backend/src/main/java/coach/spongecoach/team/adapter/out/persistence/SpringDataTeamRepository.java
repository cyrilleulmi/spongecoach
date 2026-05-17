package coach.spongecoach.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SpringDataTeamRepository extends JpaRepository<TeamJpaEntity, UUID> {
    List<TeamJpaEntity> findByClubId(UUID clubId);

    @Query("SELECT t FROM TeamJpaEntity t WHERE t.id IN " +
           "(SELECT tm.id.teamId FROM TeamMembershipJpaEntity tm WHERE tm.id.personId = :personId)")
    List<TeamJpaEntity> findByMemberId(@Param("personId") UUID personId);
}

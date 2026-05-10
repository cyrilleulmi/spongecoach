package coach.spongecoach.team.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataTeamRepository extends JpaRepository<TeamJpaEntity, UUID> {
    List<TeamJpaEntity> findByClubId(UUID clubId);
}

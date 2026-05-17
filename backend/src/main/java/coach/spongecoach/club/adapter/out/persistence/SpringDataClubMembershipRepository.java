package coach.spongecoach.club.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataClubMembershipRepository
        extends JpaRepository<ClubMembershipJpaEntity, ClubMembershipJpaEntity.ClubMembershipId> {

    Optional<ClubMembershipJpaEntity> findByIdClubIdAndIdPersonId(UUID clubId, UUID personId);

    List<ClubMembershipJpaEntity> findByIdPersonId(UUID personId);

    List<ClubMembershipJpaEntity> findByIdClubId(UUID clubId);

    boolean existsByIdClubIdAndIdPersonId(UUID clubId, UUID personId);

    void deleteByIdClubIdAndIdPersonId(UUID clubId, UUID personId);
}

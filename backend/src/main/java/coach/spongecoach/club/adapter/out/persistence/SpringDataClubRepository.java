package coach.spongecoach.club.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataClubRepository extends JpaRepository<ClubJpaEntity, UUID> {
}

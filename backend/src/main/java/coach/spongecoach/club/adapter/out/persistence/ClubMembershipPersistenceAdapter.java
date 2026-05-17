package coach.spongecoach.club.adapter.out.persistence;

import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class ClubMembershipPersistenceAdapter implements ClubMembershipRepository {

    private final SpringDataClubMembershipRepository springDataRepo;

    ClubMembershipPersistenceAdapter(SpringDataClubMembershipRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public ClubMembership save(ClubMembership membership) {
        ClubMembershipJpaEntity entity = springDataRepo
                .findByIdClubIdAndIdPersonId(membership.clubId(), membership.personId())
                .orElse(new ClubMembershipJpaEntity(membership.clubId(), membership.personId(), membership.role()));
        entity.setRole(membership.role());
        springDataRepo.save(entity);
        return membership;
    }

    @Override
    public Optional<ClubMembership> findByClubIdAndPersonId(UUID clubId, UUID personId) {
        return springDataRepo.findByIdClubIdAndIdPersonId(clubId, personId)
                .map(this::toDomain);
    }

    @Override
    public List<ClubMembership> findByPersonId(UUID personId) {
        return springDataRepo.findByIdPersonId(personId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ClubMembership> findByClubId(UUID clubId) {
        return springDataRepo.findByIdClubId(clubId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByClubIdAndPersonId(UUID clubId, UUID personId) {
        return springDataRepo.existsByIdClubIdAndIdPersonId(clubId, personId);
    }

    @Override
    @Transactional
    public void delete(UUID clubId, UUID personId) {
        springDataRepo.deleteByIdClubIdAndIdPersonId(clubId, personId);
    }

    private ClubMembership toDomain(ClubMembershipJpaEntity entity) {
        return new ClubMembership(entity.getClubId(), entity.getPersonId(), entity.getRole());
    }
}

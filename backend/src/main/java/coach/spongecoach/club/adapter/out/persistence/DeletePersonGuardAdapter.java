package coach.spongecoach.club.adapter.out.persistence;

import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.person.domain.DeletePersonGuard;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class DeletePersonGuardAdapter implements DeletePersonGuard {

    private final ClubMembershipRepository membershipRepository;
    private final ClubRepository clubRepository;

    DeletePersonGuardAdapter(ClubMembershipRepository membershipRepository, ClubRepository clubRepository) {
        this.membershipRepository = membershipRepository;
        this.clubRepository = clubRepository;
    }

    @Override
    public List<String> findBlockingClubs(UUID personId) {
        return membershipRepository.findByPersonId(personId).stream()
                .filter(m -> m.role() == ClubRole.ADMIN)
                .filter(m -> membershipRepository.findByClubId(m.clubId()).stream()
                        .filter(other -> other.role() == ClubRole.ADMIN)
                        .count() == 1)
                .map(m -> clubRepository.findById(m.clubId())
                        .map(c -> c.name())
                        .orElse(m.clubId().toString()))
                .toList();
    }
}

package coach.spongecoach.club.application;

import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.model.Club;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMembershipRepository membershipRepository;

    public ClubService(ClubRepository clubRepository, ClubMembershipRepository membershipRepository) {
        this.clubRepository = clubRepository;
        this.membershipRepository = membershipRepository;
    }

    public Club createClub(String name, String location, UUID creatorPersonId) {
        Club club = clubRepository.save(Club.create(name, location));
        membershipRepository.save(new ClubMembership(club.id(), creatorPersonId, ClubRole.ADMIN));
        return club;
    }

    @Transactional(readOnly = true)
    public Club getClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new ClubNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Club> listClubs() {
        return clubRepository.findAll();
    }

    public Club updateClub(UUID id, String name, String location) {
        Club existing = clubRepository.findById(id)
                .orElseThrow(() -> new ClubNotFoundException(id));
        return clubRepository.save(existing.withName(name).withLocation(location));
    }

    public void deleteClub(UUID id) {
        if (!clubRepository.existsById(id)) {
            throw new ClubNotFoundException(id);
        }
        clubRepository.delete(id);
    }
}

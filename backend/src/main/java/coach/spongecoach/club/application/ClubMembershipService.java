package coach.spongecoach.club.application;

import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import coach.spongecoach.club.domain.ClubRepository;
import coach.spongecoach.club.domain.TeamMembershipCleaner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClubMembershipService {

    private final ClubMembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final TeamMembershipCleaner teamMembershipCleaner;

    public ClubMembershipService(ClubMembershipRepository membershipRepository,
                                 ClubRepository clubRepository,
                                 TeamMembershipCleaner teamMembershipCleaner) {
        this.membershipRepository = membershipRepository;
        this.clubRepository = clubRepository;
        this.teamMembershipCleaner = teamMembershipCleaner;
    }

    @Transactional(readOnly = true)
    public List<ClubMembership> listMembers(UUID clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ClubNotFoundException(clubId);
        }
        return membershipRepository.findByClubId(clubId);
    }

    public ClubMembership updateRole(UUID clubId, UUID personId, ClubRole role) {
        membershipRepository.findByClubIdAndPersonId(clubId, personId)
                .orElseThrow(() -> new ClubMembershipNotFoundException(clubId, personId));
        return membershipRepository.save(new ClubMembership(clubId, personId, role));
    }

    public void removeMember(UUID clubId, UUID personId) {
        if (!membershipRepository.existsByClubIdAndPersonId(clubId, personId)) {
            throw new ClubMembershipNotFoundException(clubId, personId);
        }
        teamMembershipCleaner.removePersonFromClubTeams(clubId, personId);
        membershipRepository.delete(clubId, personId);
    }
}

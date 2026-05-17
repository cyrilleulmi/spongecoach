package coach.spongecoach.team.application;

import coach.spongecoach.club.application.NotAClubMemberException;
import coach.spongecoach.club.domain.ClubMembershipRepository;
import coach.spongecoach.team.domain.TeamMembershipRepository;
import coach.spongecoach.team.domain.TeamRepository;
import coach.spongecoach.team.domain.model.Team;
import coach.spongecoach.team.domain.model.TeamMembership;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamMembershipService {

    private final TeamMembershipRepository membershipRepository;
    private final TeamRepository teamRepository;
    private final ClubMembershipRepository clubMembershipRepository;

    public TeamMembershipService(TeamMembershipRepository membershipRepository,
                                 TeamRepository teamRepository,
                                 ClubMembershipRepository clubMembershipRepository) {
        this.membershipRepository = membershipRepository;
        this.teamRepository = teamRepository;
        this.clubMembershipRepository = clubMembershipRepository;
    }

    public TeamMembership addMember(UUID teamId, UUID personId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        if (!clubMembershipRepository.existsByClubIdAndPersonId(team.clubId(), personId)) {
            throw new NotAClubMemberException(personId, team.clubId());
        }
        if (membershipRepository.exists(teamId, personId)) {
            throw new MembershipAlreadyExistsException(teamId, personId);
        }
        return membershipRepository.save(new TeamMembership(teamId, personId));
    }

    public void removeMember(UUID teamId, UUID personId) {
        if (!membershipRepository.exists(teamId, personId)) {
            throw new MembershipNotFoundException(teamId, personId);
        }
        membershipRepository.delete(teamId, personId);
    }

    @Transactional(readOnly = true)
    public List<TeamMembership> listMembers(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
        }
        return membershipRepository.findByTeamId(teamId);
    }
}

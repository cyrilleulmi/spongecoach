package coach.spongecoach.team.application;

import coach.spongecoach.team.domain.TeamMembershipRepository;
import coach.spongecoach.team.domain.TeamRepository;
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

    public TeamMembershipService(TeamMembershipRepository membershipRepository, TeamRepository teamRepository) {
        this.membershipRepository = membershipRepository;
        this.teamRepository = teamRepository;
    }

    public TeamMembership addMember(UUID teamId, UUID personId) {
        if (!teamRepository.existsById(teamId)) {
            throw new TeamNotFoundException(teamId);
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

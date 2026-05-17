package coach.spongecoach.team.adapter.out.persistence;

import coach.spongecoach.club.domain.TeamMembershipCleaner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
class TeamMembershipCleanerAdapter implements TeamMembershipCleaner {

    private final SpringDataTeamMembershipRepository springDataRepo;

    TeamMembershipCleanerAdapter(SpringDataTeamMembershipRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    @Transactional
    public void removePersonFromClubTeams(UUID clubId, UUID personId) {
        springDataRepo.deleteByClubIdAndPersonId(clubId, personId);
    }
}

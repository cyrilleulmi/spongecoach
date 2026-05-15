package coach.spongecoach.team.adapter.out.persistence;

import coach.spongecoach.team.domain.TeamMembershipRepository;
import coach.spongecoach.team.domain.model.TeamMembership;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
class TeamMembershipPersistenceAdapter implements TeamMembershipRepository {

    private final SpringDataTeamMembershipRepository springDataRepo;

    TeamMembershipPersistenceAdapter(SpringDataTeamMembershipRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public TeamMembership save(TeamMembership membership) {
        springDataRepo.save(new TeamMembershipJpaEntity(membership.teamId(), membership.personId()));
        return membership;
    }

    @Override
    @Transactional
    public void delete(UUID teamId, UUID personId) {
        springDataRepo.deleteByIdTeamIdAndIdPersonId(teamId, personId);
    }

    @Override
    public List<TeamMembership> findByTeamId(UUID teamId) {
        return springDataRepo.findByIdTeamId(teamId).stream()
                .map(e -> new TeamMembership(e.getTeamId(), e.getPersonId()))
                .toList();
    }

    @Override
    public boolean exists(UUID teamId, UUID personId) {
        return springDataRepo.existsByIdTeamIdAndIdPersonId(teamId, personId);
    }
}

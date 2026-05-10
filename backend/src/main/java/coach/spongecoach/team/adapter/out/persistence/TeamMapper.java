package coach.spongecoach.team.adapter.out.persistence;

import coach.spongecoach.team.domain.model.Team;
import org.springframework.stereotype.Component;

@Component
class TeamMapper {

    Team toDomain(TeamJpaEntity entity) {
        return new Team(entity.getId(), entity.getName(), entity.getClubId());
    }

    TeamJpaEntity toEntity(Team team) {
        return new TeamJpaEntity(team.id(), team.name(), team.clubId());
    }
}

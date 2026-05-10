package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.team.domain.model.Team;

import java.util.UUID;

record TeamResponse(UUID id, String name, UUID clubId) {

    static TeamResponse from(Team team) {
        return new TeamResponse(team.id(), team.name(), team.clubId());
    }
}

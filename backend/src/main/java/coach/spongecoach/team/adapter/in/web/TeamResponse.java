package coach.spongecoach.team.adapter.in.web;

import coach.spongecoach.team.domain.model.Team;

import java.util.UUID;

public record TeamResponse(UUID id, String name, UUID clubId) {

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.id(), team.name(), team.clubId());
    }
}

package coach.spongecoach.team.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record UpdateTeamRequest(
        @NotBlank String name
) {}

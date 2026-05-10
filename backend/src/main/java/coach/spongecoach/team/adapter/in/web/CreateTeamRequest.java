package coach.spongecoach.team.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateTeamRequest(
        @NotBlank String name,
        @NotNull UUID clubId
) {}

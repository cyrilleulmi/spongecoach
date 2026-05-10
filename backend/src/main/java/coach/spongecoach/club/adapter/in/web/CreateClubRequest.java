package coach.spongecoach.club.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record CreateClubRequest(
        @NotBlank String name,
        @NotBlank String location
) {}

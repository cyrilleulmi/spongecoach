package coach.spongecoach.club.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record UpdateClubRequest(
        @NotBlank String name,
        @NotBlank String location
) {}

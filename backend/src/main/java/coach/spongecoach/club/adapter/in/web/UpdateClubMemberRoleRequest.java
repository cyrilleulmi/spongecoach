package coach.spongecoach.club.adapter.in.web;

import coach.spongecoach.auth.domain.model.ClubRole;
import jakarta.validation.constraints.NotNull;

record UpdateClubMemberRoleRequest(@NotNull ClubRole role) {
}

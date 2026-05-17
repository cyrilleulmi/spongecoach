package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.ClubMembership;
import coach.spongecoach.auth.domain.model.ClubRole;

import java.util.List;
import java.util.UUID;

record MockUserResponse(UUID personId, String email, String role, List<ClubMembership> clubMemberships) {
    static MockUserResponse from(AuthenticatedUser user) {
        String role = user.clubMemberships().stream()
                .anyMatch(m -> m.role() == ClubRole.ADMIN) ? "ADMIN" : "USER";
        return new MockUserResponse(user.personId(), user.email(), role, user.clubMemberships());
    }
}

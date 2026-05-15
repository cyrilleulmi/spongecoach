package coach.spongecoach.auth.adapter.in.web;

import coach.spongecoach.auth.domain.model.AuthenticatedUser;
import coach.spongecoach.auth.domain.model.Role;

import java.util.UUID;

record MockUserResponse(UUID personId, String email, Role role) {
    static MockUserResponse from(AuthenticatedUser user) {
        return new MockUserResponse(user.personId(), user.email(), user.role());
    }
}

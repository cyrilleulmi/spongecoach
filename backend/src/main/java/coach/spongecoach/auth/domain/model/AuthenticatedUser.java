package coach.spongecoach.auth.domain.model;

import java.util.UUID;

public record AuthenticatedUser(UUID personId, String email, Role role) {
}

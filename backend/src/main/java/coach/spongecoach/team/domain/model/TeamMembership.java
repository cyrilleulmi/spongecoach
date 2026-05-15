package coach.spongecoach.team.domain.model;

import java.util.UUID;

public record TeamMembership(UUID teamId, UUID personId) {
}

package coach.spongecoach.auth.domain.model;

import java.util.UUID;

public record ClubMembership(UUID clubId, UUID personId, ClubRole role) {
}

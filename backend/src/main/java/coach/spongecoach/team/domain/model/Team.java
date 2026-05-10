package coach.spongecoach.team.domain.model;

import java.util.UUID;

public record Team(UUID id, String name, UUID clubId) {

    public static Team create(String name, UUID clubId) {
        return new Team(UUID.randomUUID(), name, clubId);
    }

    public Team withName(String name) {
        return new Team(this.id, name, this.clubId);
    }
}

package coach.spongecoach.club.domain.model;

import java.util.UUID;

public record Club(UUID id, String name, String location) {

    public static Club create(String name, String location) {
        return new Club(UUID.randomUUID(), name, location);
    }

    public Club withName(String name) {
        return new Club(this.id, name, this.location);
    }

    public Club withLocation(String location) {
        return new Club(this.id, this.name, location);
    }
}
